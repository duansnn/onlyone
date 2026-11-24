package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.StockAdjustDTO;
import com.owner.system.mall.domain.dto.StockSafeDTO;
import com.owner.system.mall.domain.dto.StockTakeCreateDTO;
import com.owner.system.mall.domain.dto.StockTakeSubmitDTO;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.entity.StockLog;
import com.owner.system.mall.domain.entity.StockTake;
import com.owner.system.mall.domain.entity.StockTakeItem;
import com.owner.system.mall.domain.enums.StockReason;
import com.owner.system.mall.domain.vo.SkuStockVO;
import com.owner.system.mall.domain.vo.StockLogVO;
import com.owner.system.mall.domain.vo.StockTakeVO;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import com.owner.system.mall.mapper.StockLogMapper;
import com.owner.system.mall.mapper.StockTakeItemMapper;
import com.owner.system.mall.mapper.StockTakeMapper;
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
    private static final int EXPORT_MAX = 5000;

    private final ProductSkuMapper skuMapper;
    private final StockLogMapper stockLogMapper;
    private final ProductMapper productMapper;
    private final StockTakeMapper stockTakeMapper;
    private final StockTakeItemMapper stockTakeItemMapper;
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
                    sku.getStock(), sku.getLockedStock(), sku.availableStock(), sku.getSafeStock(),
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
            // 商品删了，历史流水还得能读出当时是什么规格，所以这里不过滤 del_flag
            for (ProductSku sku : skuMapper.selectByIdsIgnoreDeleted(skuIds)) {
                skuMap.put(sku.getId(), sku);
            }
        }
        Map<Long, String> productNames = productNames(skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().toList(), true);

        List<StockLogVO> rows = new ArrayList<>(page.getRecords().size());
        for (StockLog item : page.getRecords()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            StockReason reason = StockReason.of(item.getReason());
            String productName = sku == null ? null : productNames.get(sku.getProductId());
            rows.add(new StockLogVO(item.getId(), item.getSkuId(),
                    productName == null ? "已删除的商品" : productName,
                    sku == null ? "规格已删除" : sku.getSpecName(),
                    item.getChangeQty(), item.getAfterStock(), item.getAfterLocked(),
                    item.getReason(), reason == null ? item.getReason() : reason.getLabel(),
                    item.getOrderId(), item.getOperatorId(), item.getRemark(), item.getCreateTime()));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 导出 SKU 库存 Excel：与 pageSku 同筛选，最多导出 5000 个规格 */
    public byte[] exportSkus(Long shopId, String keyword, Long productId) {
        shopGuard.requireMember(shopId);
        List<ProductSku> skus = skuMapper.selectList(
                Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getShopId, shopId)
                        .eq(productId != null, ProductSku::getProductId, productId)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(ProductSku::getSpecName, keyword)
                                .or().like(ProductSku::getSkuCode, keyword))
                        .orderByAsc(ProductSku::getProductId)
                        .orderByAsc(ProductSku::getId)
                        .last("LIMIT " + (EXPORT_MAX + 1)));
        if (skus.isEmpty()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "当前筛选没有可导出的库存");
        }
        if (skus.size() > EXPORT_MAX) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "一次最多导出 " + EXPORT_MAX + " 个规格，请缩小范围");
        }
        Map<Long, String> productNames = productNames(skus.stream()
                .map(ProductSku::getProductId).distinct().toList());
        List<SkuStockVO> rows = new ArrayList<>(skus.size());
        for (ProductSku sku : skus) {
            rows.add(new SkuStockVO(sku.getId(), sku.getProductId(),
                    productNames.get(sku.getProductId()), sku.getSpecName(), sku.getSkuCode(),
                    sku.getStock(), sku.getLockedStock(), sku.availableStock(), sku.getSafeStock(),
                    sku.getStatus(), ProductAssembler.skuStatusLabel(sku.getStatus())));
        }
        return SkuStockExcel.write(rows);
    }

    /** 导出库存流水 Excel：与 pageLogs 同筛选，最多导出 5000 条 */
    public byte[] exportLogs(Long shopId, Long skuId) {
        shopGuard.requireMember(shopId);
        List<StockLog> logs = stockLogMapper.selectList(
                Wrappers.<StockLog>lambdaQuery()
                        .eq(StockLog::getShopId, shopId)
                        .eq(skuId != null, StockLog::getSkuId, skuId)
                        .orderByDesc(StockLog::getCreateTime)
                        .orderByDesc(StockLog::getId)
                        .last("LIMIT " + (EXPORT_MAX + 1)));
        if (logs.isEmpty()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "当前筛选没有可导出的流水");
        }
        if (logs.size() > EXPORT_MAX) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "一次最多导出 " + EXPORT_MAX + " 条流水，请缩小范围");
        }

        Map<Long, ProductSku> skuMap = new HashMap<>();
        List<Long> skuIds = logs.stream().map(StockLog::getSkuId).distinct().toList();
        if (!skuIds.isEmpty()) {
            for (ProductSku sku : skuMapper.selectByIdsIgnoreDeleted(skuIds)) {
                skuMap.put(sku.getId(), sku);
            }
        }
        Map<Long, String> productNames = productNames(skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().toList(), true);

        List<StockLogVO> rows = new ArrayList<>(logs.size());
        for (StockLog item : logs) {
            ProductSku sku = skuMap.get(item.getSkuId());
            StockReason reason = StockReason.of(item.getReason());
            String productName = sku == null ? null : productNames.get(sku.getProductId());
            rows.add(new StockLogVO(item.getId(), item.getSkuId(),
                    productName == null ? "已删除的商品" : productName,
                    sku == null ? "规格已删除" : sku.getSpecName(),
                    item.getChangeQty(), item.getAfterStock(), item.getAfterLocked(),
                    item.getReason(), reason == null ? item.getReason() : reason.getLabel(),
                    item.getOrderId(), item.getOperatorId(), item.getRemark(), item.getCreateTime()));
        }
        return StockLogExcel.write(rows);
    }

    // ------------------------------------------------------------------
    // 库存预警 + 盘点
    // ------------------------------------------------------------------

    /** 库存预警：可售库存 <= 安全阈值（且设置了阈值）的 SKU，按可售升序 */
    public List<SkuStockVO> warnings(Long shopId) {
        shopGuard.requireMember(shopId);
        List<ProductSku> skus = skuMapper.selectList(
                Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getShopId, shopId)
                        .isNotNull(ProductSku::getSafeStock));
        Map<Long, String> productNames = productNames(skus.stream()
                .map(ProductSku::getProductId).distinct().toList());
        List<SkuStockVO> result = new ArrayList<>();
        for (ProductSku sku : skus) {
            if (sku.availableStock() <= sku.getSafeStock()) {
                result.add(new SkuStockVO(sku.getId(), sku.getProductId(),
                        productNames.get(sku.getProductId()), sku.getSpecName(), sku.getSkuCode(),
                        sku.getStock(), sku.getLockedStock(), sku.availableStock(), sku.getSafeStock(),
                        sku.getStatus(), ProductAssembler.skuStatusLabel(sku.getStatus())));
            }
        }
        result.sort((a, b) -> Integer.compare(a.availableStock(), b.availableStock()));
        return result;
    }

    /** 设置安全库存阈值（safeStock 为 null 表示取消预警） */
    public void setSafeStock(Long shopId, StockSafeDTO dto) {
        shopGuard.requireMember(shopId);
        ProductSku sku = skuMapper.selectById(dto.skuId());
        if (sku == null || !shopId.equals(sku.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "规格不存在：" + dto.skuId());
        }
        if (dto.safeStock() != null && dto.safeStock() < 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "安全库存阈值不能为负数");
        }
        sku.setSafeStock(dto.safeStock());
        if (skuMapper.updateById(sku) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
    }

    /** 新建盘点单（草稿） */
    public StockTakeVO createTake(Long shopId, StockTakeCreateDTO dto) {
        shopGuard.requireMember(shopId);
        StockTake take = new StockTake();
        take.setShopId(shopId);
        take.setTitle(dto.title().trim());
        take.setStatus("DRAFT");
        take.setOperatorId(MallCurrentUser.requireUserId());
        take.setRemark(dto.remark());
        stockTakeMapper.insert(take);
        return toVO(take, Collections.emptyList());
    }

    /** 提交盘点：逐条按「实盘 - 账面」生成差异调整，并把盘点单置为 DONE */
    @Transactional(rollbackFor = Exception.class)
    public StockTakeVO submitTake(Long shopId, Long takeId, StockTakeSubmitDTO dto) {
        shopGuard.requireMember(shopId);
        StockTake take = stockTakeMapper.selectById(takeId);
        if (take == null || !shopId.equals(take.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "盘点单不存在：" + takeId);
        }
        if ("DONE".equals(take.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该盘点单已完成，不能重复提交");
        }
        Long operatorId = MallCurrentUser.requireUserId();

        List<StockTakeItem> items = new ArrayList<>();
        for (StockTakeSubmitDTO.Item item : dto.items()) {
            ProductSku sku = skuMapper.selectById(item.skuId());
            if (sku == null || !shopId.equals(sku.getShopId())) {
                throw new ServiceException(ResultCode.DATA_NOT_FOUND, "规格不存在：" + item.skuId());
            }
            if (item.actualStock() < 0) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR, "实盘库存不能为负数");
            }
            int book = sku.getStock() == null ? 0 : sku.getStock();
            int diff = item.actualStock() - book;

            StockTakeItem entity = new StockTakeItem();
            entity.setTakeId(takeId);
            entity.setShopId(shopId);
            entity.setSkuId(item.skuId());
            entity.setBookStock(book);
            entity.setActualStock(item.actualStock());
            entity.setDiffQty(diff);
            stockTakeItemMapper.insert(entity);
            items.add(entity);

            // 有差异才调整库存（并记流水）
            if (diff != 0) {
                int locked = sku.getLockedStock() == null ? 0 : sku.getLockedStock();
                if (item.actualStock() < locked) {
                    throw new ServiceException(ResultCode.BUSINESS_ERROR,
                            "实盘库存不能小于未支付订单锁定量 " + locked);
                }
                sku.setStock(item.actualStock());
                if (skuMapper.updateById(sku) == 0) {
                    throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
                }
                writeLog(shopId, item.skuId(), diff, item.actualStock(), locked,
                        StockReason.TAKE, null, operatorId, "盘点「" + take.getTitle() + "」差异 " + (diff > 0 ? "+" : "") + diff);
            }
        }

        take.setStatus("DONE");
        stockTakeMapper.updateById(take);
        return toVO(take, items);
    }

    /** 盘点单列表（含明细） */
    public List<StockTakeVO> listTakes(Long shopId) {
        shopGuard.requireMember(shopId);
        List<StockTake> takes = stockTakeMapper.selectList(
                Wrappers.<StockTake>lambdaQuery()
                        .eq(StockTake::getShopId, shopId)
                        .orderByDesc(StockTake::getCreateTime));
        if (takes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> takeIds = takes.stream().map(StockTake::getId).toList();
        List<StockTakeItem> allItems = stockTakeItemMapper.selectList(
                Wrappers.<StockTakeItem>lambdaQuery().in(StockTakeItem::getTakeId, takeIds));

        Map<Long, List<StockTakeItem>> byTake = new HashMap<>();
        for (StockTakeItem item : allItems) {
            byTake.computeIfAbsent(item.getTakeId(), k -> new ArrayList<>()).add(item);
        }

        List<StockTakeVO> result = new ArrayList<>();
        for (StockTake take : takes) {
            result.add(toVO(take, byTake.getOrDefault(take.getId(), Collections.emptyList())));
        }
        return result;
    }

    /** 组装盘点单 VO */
    private StockTakeVO toVO(StockTake take, List<StockTakeItem> items) {
        List<Long> skuIds = items.stream().map(StockTakeItem::getSkuId).distinct().toList();
        Map<Long, ProductSku> skuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            for (ProductSku sku : skuMapper.selectByIdsIgnoreDeleted(skuIds)) {
                skuMap.put(sku.getId(), sku);
            }
        }
        Map<Long, String> productNames = productNames(skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().toList(), true);

        List<StockTakeVO.ItemVO> itemVOs = new ArrayList<>();
        for (StockTakeItem item : items) {
            ProductSku sku = skuMap.get(item.getSkuId());
            String productName = sku == null ? "已删除的商品" : productNames.get(sku.getProductId());
            itemVOs.add(new StockTakeVO.ItemVO(item.getSkuId(),
                    productName == null ? "已删除的商品" : productName,
                    sku == null ? "规格已删除" : sku.getSpecName(),
                    item.getBookStock(), item.getActualStock(), item.getDiffQty()));
        }
        return new StockTakeVO(take.getId(), take.getTitle(), take.getStatus(),
                "DONE".equals(take.getStatus()) ? "已完成" : "草稿",
                take.getRemark(), take.getCreateTime(), itemVOs);
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
        return productNames(productIds, false);
    }

    /**
     * @param includeDeleted 为 true 时连已删除商品一起查（库存流水等历史数据要能显示名字）
     */
    private Map<Long, String> productNames(List<Long> productIds, boolean includeDeleted) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>(productIds.size() * 2);
        List<Product> products = includeDeleted
                ? productMapper.selectByIdsIgnoreDeleted(productIds)
                : productMapper.selectList(Wrappers.<Product>lambdaQuery().in(Product::getId, productIds));
        for (Product product : products) {
            result.put(product.getId(), product.getName());
        }
        return result;
    }

    /** 库存是否启用（供其它服务判断加购资格） */
    public static boolean isEnabled(Integer status) {
        return status == null || status == STATUS_ENABLED;
    }
}
