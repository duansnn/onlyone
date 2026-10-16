package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.StockAdjustDTO;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.entity.StockLog;
import com.owner.system.mall.domain.enums.StockReason;
import com.owner.system.mall.domain.vo.SkuStockVO;
import com.owner.system.mall.domain.vo.StockLogVO;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import com.owner.system.mall.mapper.StockLogMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ProductAssembler;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存服务。
 *
 * <p>库存只在 SKU 上，可售 = 库存 − 未支付订单锁定量。锁定与扣减都用带条件的
 * 原子 UPDATE 完成（见 {@link ProductSkuMapper}），避免「先查后改」造成超卖。</p>
 *
 * <p>每次变化都写一条 {@code stock_log}，原因限定为
 * 初始入库 / 手动调整 / 下单锁定 / 支付扣减 / 取消释放 / 售后退回。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final int STATUS_ENABLED = 0;

    private final ProductSkuMapper skuMapper;
    private final StockLogMapper stockLogMapper;
    private final ProductMapper productMapper;
    private final ShopGuard shopGuard;
    private final ProductAssembler productAssembler;

    // ------------------------------------------------------------------
    // 订单流程使用（由 OrderService 在事务内调用）
    // ------------------------------------------------------------------

    /** 下单锁定：可售不足直接失败，整单回滚 */
    public void lock(Long shopId, Long skuId, int qty, Long orderId, Long operatorId) {
        if (skuMapper.lockStock(skuId, qty) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "库存不足，无法提交订单");
        }
        ProductSku sku = skuMapper.selectById(skuId);
        writeLog(shopId, skuId, 0, sku.getStock(), sku.getLockedStock(),
                StockReason.LOCK, orderId, operatorId, "下单锁定 " + qty);
    }

    /** 取消 / 超时释放锁定。已释放过则跳过（幂等）。 */
    public void release(Long shopId, Long skuId, int qty, Long orderId, Long operatorId) {
        if (skuMapper.releaseLockedStock(skuId, qty) == 0) {
            log.warn("[Stock] 释放锁定未命中，可能已释放 skuId={}, qty={}, orderId={}", skuId, qty, orderId);
            return;
        }
        ProductSku sku = skuMapper.selectById(skuId);
        writeLog(shopId, skuId, 0, sku.getStock(), sku.getLockedStock(),
                StockReason.CANCEL, orderId, operatorId, "取消释放 " + qty);
    }

    /** 支付成功：锁定转扣减 */
    public void confirmPaid(Long shopId, Long skuId, int qty, Long orderId, Long operatorId) {
        if (skuMapper.confirmLockedStock(skuId, qty) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "库存锁定状态异常，无法扣减");
        }
        ProductSku sku = skuMapper.selectById(skuId);
        writeLog(shopId, skuId, -qty, sku.getStock(), sku.getLockedStock(),
                StockReason.PAY, orderId, operatorId, "支付扣减 " + qty);
    }

    /** 售后退回：库存加回去 */
    public void giveBack(Long shopId, Long skuId, int qty, Long orderId, Long operatorId) {
        skuMapper.increaseStock(skuId, qty);
        ProductSku sku = skuMapper.selectById(skuId);
        writeLog(shopId, skuId, qty, sku.getStock(), sku.getLockedStock(),
                StockReason.RETURN, orderId, operatorId, "售后退回 " + qty);
    }

    /** 新建规格时记一条初始入库（由商品服务调用） */
    public void recordInit(Long shopId, Long skuId, int stock, Long operatorId) {
        if (stock <= 0) {
            return;
        }
        writeLog(shopId, skuId, stock, stock, 0, StockReason.INIT, null, operatorId, "初始入库");
    }

    /** 编辑规格时库存发生变化，记一条手动调整（由商品服务调用） */
    public void recordManual(Long shopId, Long skuId, int changeQty, int afterStock,
                             int afterLocked, Long operatorId, String remark) {
        writeLog(shopId, skuId, changeQty, afterStock, afterLocked,
                StockReason.MANUAL, null, operatorId, remark);
    }

    // ------------------------------------------------------------------
    // 后台库存页
    // ------------------------------------------------------------------

    /** 库存调整：填调整后的数量与原因 */
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long shopId, StockAdjustDTO dto) {
        shopGuard.requireMember(shopId);
        ProductSku sku = skuMapper.selectById(dto.skuId());
        if (sku == null || !shopId.equals(sku.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "规格不存在：" + dto.skuId());
        }
        int locked = sku.getLockedStock() == null ? 0 : sku.getLockedStock();
        int before = sku.getStock() == null ? 0 : sku.getStock();
        if (dto.stock() < locked) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "调整后库存不能小于未支付订单锁定量 " + locked + "，否则可售库存会变成负数");
        }
        int delta = dto.stock() - before;
        if (delta == 0) {
            return;
        }
        sku.setStock(dto.stock());
        if (skuMapper.updateById(sku) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        StockReason reason = StockReason.of(dto.reason());
        if (reason != StockReason.INIT && reason != StockReason.MANUAL) {
            reason = StockReason.MANUAL;
        }
        writeLog(shopId, sku.getId(), delta, dto.stock(), locked, reason, null,
                MallCurrentUser.requireUserId(), dto.remark());
        log.info("[Stock] 调整库存 shopId={}, skuId={}, {} -> {}", shopId, sku.getId(), before, dto.stock());
    }

    /** SKU 库存分页 */
    public PageResult<SkuStockVO> pageSku(Long shopId, String keyword, Long productId, PageQuery query) {
        shopGuard.requireMember(shopId);
        Page<ProductSku> page = skuMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getShopId, shopId)
                        .eq(productId != null, ProductSku::getProductId, productId)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(ProductSku::getSpecName, keyword)
                                .or().like(ProductSku::getSkuCode, keyword))
                        .orderByAsc(ProductSku::getProductId)
                        .orderByAsc(ProductSku::getId));

        Map<Long, String> productNames = productNames(page.getRecords().stream()
                .map(ProductSku::getProductId).distinct().toList());

        List<SkuStockVO> rows = new ArrayList<>(page.getRecords().size());
        for (ProductSku sku : page.getRecords()) {
            rows.add(new SkuStockVO(sku.getId(), sku.getProductId(),
                    productNames.get(sku.getProductId()), sku.getSpecName(), sku.getSkuCode(),
                    sku.getStock(), sku.getLockedStock(), sku.availableStock(),
                    sku.getStatus(), ProductAssembler.skuStatusLabel(sku.getStatus())));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 库存流水分页 */
    public PageResult<StockLogVO> pageLogs(Long shopId, Long skuId, PageQuery query) {
        shopGuard.requireMember(shopId);
        Page<StockLog> page = stockLogMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<StockLog>lambdaQuery()
                        .eq(StockLog::getShopId, shopId)
                        .eq(skuId != null, StockLog::getSkuId, skuId)
                        .orderByDesc(StockLog::getCreateTime)
                        .orderByDesc(StockLog::getId));

        Map<Long, ProductSku> skuMap = new HashMap<>();
        List<Long> skuIds = page.getRecords().stream().map(StockLog::getSkuId).distinct().toList();
        if (!skuIds.isEmpty()) {
            for (ProductSku sku : skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                    .in(ProductSku::getId, skuIds))) {
                skuMap.put(sku.getId(), sku);
            }
        }
        Map<Long, String> productNames = productNames(skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().toList());

        List<StockLogVO> rows = new ArrayList<>(page.getRecords().size());
        for (StockLog item : page.getRecords()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            StockReason reason = StockReason.of(item.getReason());
            rows.add(new StockLogVO(item.getId(), item.getSkuId(),
                    sku == null ? null : productNames.get(sku.getProductId()),
                    sku == null ? null : sku.getSpecName(),
                    item.getChangeQty(), item.getAfterStock(), item.getAfterLocked(),
                    item.getReason(), reason == null ? item.getReason() : reason.getLabel(),
                    item.getOrderId(), item.getOperatorId(), item.getRemark(), item.getCreateTime()));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private void writeLog(Long shopId, Long skuId, int changeQty, int afterStock, int afterLocked,
                          StockReason reason, Long orderId, Long operatorId, String remark) {
        StockLog entity = new StockLog();
        entity.setShopId(shopId);
        entity.setSkuId(skuId);
        entity.setChangeQty(changeQty);
        entity.setAfterStock(afterStock);
        entity.setAfterLocked(afterLocked);
        entity.setReason(reason.name());
        entity.setOrderId(orderId);
        entity.setOperatorId(operatorId);
        entity.setRemark(remark);
        stockLogMapper.insert(entity);
    }

    private Map<Long, String> productNames(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>(productIds.size() * 2);
        for (Product product : productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .in(Product::getId, productIds))) {
            result.put(product.getId(), product.getName());
        }
        return result;
    }

    /** 库存是否启用（供其它服务判断加购资格） */
    public static boolean isEnabled(Integer status) {
        return status == null || status == STATUS_ENABLED;
    }
}
