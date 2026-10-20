package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.OrderCancelDTO;
import com.owner.system.mall.domain.dto.OrderPreviewDTO;
import com.owner.system.mall.domain.dto.OrderSkuDTO;
import com.owner.system.mall.domain.dto.OrderSubmitDTO;
import com.owner.system.mall.domain.dto.ShipDTO;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.OrderLog;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.entity.Refund;
import com.owner.system.mall.domain.entity.Review;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.entity.UserAddress;
import com.owner.system.mall.domain.enums.OperatorType;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.PayChannel;
import com.owner.system.mall.domain.enums.ProductStatus;
import com.owner.system.mall.domain.enums.RefundStatus;
import com.owner.system.mall.domain.enums.ShopRole;
import com.owner.system.mall.domain.vo.CartItemVO;
import com.owner.system.mall.domain.vo.CouponUserVO;
import com.owner.system.mall.domain.vo.OrderDetailVO;
import com.owner.system.mall.domain.vo.OrderItemVO;
import com.owner.system.mall.domain.vo.OrderLogVO;
import com.owner.system.mall.domain.vo.OrderPreviewVO;
import com.owner.system.mall.domain.vo.OrderSubmitVO;
import com.owner.system.mall.domain.vo.OrderVO;
import com.owner.system.mall.mapper.OrderInfoMapper;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.OrderLogMapper;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import com.owner.system.mall.mapper.RefundMapper;
import com.owner.system.mall.mapper.ReviewMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ProductAssembler;
import com.owner.system.mall.support.RefundVos;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单服务：计价、提交、锁定库存、取消、确认收货、后台发货，以及超时/自动完成的批量处理。
 *
 * <p>金额一律为分。单价以提交订单时的 SKU 价格为准并写入订单行，之后改价不影响这笔单。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** 待付款超时分钟数 */
    public static final int PAY_TIMEOUT_MINUTES = 30;

    /** 发货后自动确认收货天数 */
    public static final int AUTO_FINISH_DAYS = 7;

    /** 确认收货后可申请售后的天数 */
    public static final int REFUND_WINDOW_DAYS = 7;

    /** 常用快递公司 */
    public static final List<String> SHIP_COMPANIES = List.of(
            "顺丰", "中通", "圆通", "韵达", "申通", "京东", "邮政", "其他");

    /** 取消订单的原因 */
    public static final List<String> CANCEL_REASONS = List.of("不想要了", "信息填错", "重新下单");

    private static final int SKU_ENABLED = 0;
    private static final int MAX_QUANTITY = 99;

    /** 店铺成员状态：停用 */
    private static final int MEMBER_DISABLED = 1;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ShopMapper shopMapper;
    private final RefundMapper refundMapper;
    private final ReviewMapper reviewMapper;
    private final com.owner.system.mall.mapper.UserAddressMapper addressMapper;
    private final ShopGuard shopGuard;
    private final CartService cartService;
    private final CouponService couponService;
    private final StockService stockService;
    private final ProductAssembler productAssembler;

    /**
     * 是否允许店铺成员（店主/店员）在自己店铺下单。
     *
     * <p>默认 false：自购会刷出虚假销量、让「自己领券自己核销」变成套利，账目也对不平。
     * 本地想用店主账号跑通购买流程时，加 {@code --owner.mall.order.allow-self-purchase=true} 或改 yml。</p>
     */
    @Value("${owner.mall.order.allow-self-purchase:false}")
    private boolean allowSelfPurchase;

    // ==================================================================
    // 顾客侧：计价 / 提交 / 列表 / 详情 / 取消 / 确认收货
    // ==================================================================

    /** 确认订单页计价 */
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        Shop shop = shopGuard.loadActiveShop(dto.shopId());

        BuildResult build = build(shop.getId(), dto.items(), false);
        long goods = build.goodsAmount();
        long freight = 0L;

        long discount = 0L;
        String couponName = null;
        if (dto.couponUserId() != null && !build.lines().isEmpty()) {
            CouponChoice choice = chooseCoupon(userId, shop.getId(), dto.couponUserId(), goods);
            discount = choice.discount();
            couponName = choice.name();
        }
        long pay = Math.max(goods + freight - discount, 0L);

        List<CouponUserVO> usable = build.lines().isEmpty()
                ? List.of()
                : couponService.usableCoupons(userId, shop.getId(), goods);

        String message = build.invalid().isEmpty() ? null : "部分商品已下架或缺货，已为你移除";
        if (build.priceChanged()) {
            message = "商品价格有更新，请确认后再提交";
        }
        // 店主/店员自购：预览页先提示清楚，提交时再硬拦
        String selfPurchase = selfPurchaseNotice(shop, userId);
        if (selfPurchase != null) {
            message = selfPurchase;
        }

        return new OrderPreviewVO(shop.getId(), shop.getName(), build.itemVos(), build.invalid(),
                goods, freight, discount, pay, discount > 0 ? dto.couponUserId() : null,
                couponName, usable, message);
    }

    /** 提交订单：生成待付款订单并锁定库存 */
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrderSubmitDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        Shop shop = shopGuard.loadActiveShop(dto.shopId());

        String selfPurchase = selfPurchaseNotice(shop, userId);
        if (selfPurchase != null) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, selfPurchase);
        }

        BuildResult build = build(shop.getId(), dto.items(), true);
        if (!build.invalid().isEmpty()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, build.invalid().get(0).invalidReason());
        }
        if (build.priceChanged()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "商品价格已更新，请返回确认后重新提交");
        }
        if (build.lines().isEmpty()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "下单商品不能为空");
        }

        UserAddress address = requireOwnAddress(userId, dto.addressId());

        long goods = build.goodsAmount();
        long freight = 0L;
        long discount = 0L;
        String couponName = null;

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateOrderNo(shop.getId()));
        order.setShopId(shop.getId());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setGoodsAmount(goods);
        order.setFreightAmount(freight);
        order.setReceiverName(address.getReceiver());
        order.setReceiverMobile(address.getMobile());
        order.setReceiverAddress(AddressService.fullAddress(address));
        order.setBuyerRemark(StringUtils.trimToNull(dto.buyerRemark()));
        order.setExpireTime(LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES));
        order.setDiscountAmount(0L);
        order.setPayAmount(goods);
        order.setVersion(0);
        orderInfoMapper.insert(order);

        // 占用优惠券（此时才有 orderId 可写）
        if (dto.couponUserId() != null) {
            CouponChoice choice = chooseCoupon(userId, shop.getId(), dto.couponUserId(), goods);
            discount = choice.discount();
            couponName = choice.name();
            order.setCouponId(choice.couponId());
            order.setCouponUserId(dto.couponUserId());
            couponService.lock(userId, shop.getId(), dto.couponUserId(), goods, order.getId());
        }
        long pay = Math.max(goods + freight - discount, 0L);
        order.setDiscountAmount(discount);
        order.setPayAmount(pay);
        orderInfoMapper.updateById(order);

        // 订单行快照
        List<Long> skuIds = new ArrayList<>(build.lines().size());
        for (Line line : build.lines()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setShopId(shop.getId());
            item.setProductId(line.product().getId());
            item.setSkuId(line.sku().getId());
            item.setProductName(line.product().getName());
            item.setSpecName(line.sku().getSpecName());
            item.setImageUrl(line.imageUrl());
            item.setPrice(line.price());
            item.setQuantity(line.quantity());
            item.setAmount(line.price() * line.quantity());
            orderItemMapper.insert(item);
            skuIds.add(line.sku().getId());
        }

        // 锁定库存：任何一行不足都会整单回滚
        for (Line line : build.lines()) {
            stockService.lock(shop.getId(), line.sku().getId(), line.quantity(), order.getId(), userId);
        }

        writeLog(order.getId(), null, OrderStatus.PENDING_PAYMENT.name(), userId,
                OperatorType.CUSTOMER, "提交订单");

        // 已下单的行从购物车移除
        cartService.removeOwned(userId, skuIds);

        log.info("[Order] 下单成功 orderId={}, orderNo={}, goods={}, discount={}, pay={}, coupon={}",
                order.getId(), order.getOrderNo(), goods, discount, pay, couponName);
        return new OrderSubmitVO(order.getId(), order.getOrderNo(), pay, order.getExpireTime());
    }

    /** 顾客订单列表 */
    public PageResult<OrderVO> myOrders(String status, PageQuery query) {
        Long userId = MallCurrentUser.requireUserId();
        OrderStatus statusFilter = OrderStatus.of(status);
        Page<OrderInfo> page = orderInfoMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<OrderInfo>lambdaQuery()
                        .eq(OrderInfo::getUserId, userId)
                        .eq(statusFilter != null, OrderInfo::getStatus,
                                statusFilter == null ? null : statusFilter.name())
                        .orderByDesc(OrderInfo::getCreateTime));
        return PageResult.of(toVoList(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize());
    }

    /** 顾客订单详情 */
    public OrderDetailVO myOrderDetail(Long orderId) {
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        return toDetailVo(order);
    }

    /** 取消待付款订单 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, OrderCancelDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待付款的订单可以取消");
        }
        String reason = dto == null || StringUtils.isBlank(dto.reason()) ? "不想要了" : dto.reason().trim();
        cancelInternal(order, reason, userId, OperatorType.CUSTOMER);
    }

    /** 确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long orderId) {
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.PENDING_RECEIPT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待收货的订单可以确认收货");
        }
        String from = order.getStatus();
        order.setStatus(OrderStatus.FINISHED.name());
        order.setFinishTime(LocalDateTime.now());
        saveOrder(order);
        writeLog(orderId, from, OrderStatus.FINISHED.name(), userId, OperatorType.CUSTOMER, "确认收货");
        log.info("[Order] 确认收货 orderId={}", orderId);
    }

    // ==================================================================
    // 店铺后台：列表 / 详情 / 发货
    // ==================================================================

    /** 后台订单列表 */
    public PageResult<OrderVO> shopOrders(Long shopId, String status, String orderNo,
                                          String receiverMobile, String dateFrom, String dateTo,
                                          PageQuery query) {
        shopGuard.requireMember(shopId);
        OrderStatus statusFilter = OrderStatus.of(status);
        LocalDateTime from = parseStartOfDay(dateFrom);
        LocalDateTime to = parseEndOfDay(dateTo);
        Page<OrderInfo> page = orderInfoMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<OrderInfo>lambdaQuery()
                        .eq(OrderInfo::getShopId, shopId)
                        .eq(statusFilter != null, OrderInfo::getStatus,
                                statusFilter == null ? null : statusFilter.name())
                        .eq(StringUtils.isNotBlank(orderNo), OrderInfo::getOrderNo, orderNo)
                        .eq(StringUtils.isNotBlank(receiverMobile), OrderInfo::getReceiverMobile, receiverMobile)
                        .ge(from != null, OrderInfo::getCreateTime, from)
                        .le(to != null, OrderInfo::getCreateTime, to)
                        .orderByDesc(OrderInfo::getCreateTime));
        return PageResult.of(toVoList(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize());
    }

    /** 后台订单详情（店主与店员能看完整手机号，因为要发货） */
    public OrderDetailVO shopOrderDetail(Long shopId, Long orderId) {
        shopGuard.requireMember(shopId);
        OrderInfo order = requireOrder(orderId);
        if (!shopId.equals(order.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        return toDetailVo(order);
    }

    /** 发货：填快递公司与运单号 */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long shopId, Long orderId, ShipDTO dto) {
        shopGuard.requireMember(shopId);
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = requireShopOrder(shopId, orderId);

        if (!OrderStatus.PENDING_SHIPMENT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待发货的订单可以发货");
        }
        // 顾客已申请退款时先处理售后
        if (hasProcessingRefund(orderId)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该订单有未处理完的售后，请先处理售后");
        }

        String from = order.getStatus();
        order.setShipCompany(dto.shipCompany().trim());
        order.setShipNo(dto.shipNo().trim());
        order.setShipTime(LocalDateTime.now());
        order.setShipUserId(userId);
        order.setStatus(OrderStatus.PENDING_RECEIPT.name());
        saveOrder(order);
        writeLog(orderId, from, OrderStatus.PENDING_RECEIPT.name(), userId, OperatorType.SHOP,
                "已发货：" + order.getShipCompany() + " " + order.getShipNo());
        log.info("[Order] 发货 orderId={}, company={}, no={}", orderId, order.getShipCompany(), order.getShipNo());
    }

    /** 改运单号：待收货状态只能改一次 */
    @Transactional(rollbackFor = Exception.class)
    public void updateShip(Long shopId, Long orderId, ShipDTO dto) {
        shopGuard.requireMember(shopId);
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = requireShopOrder(shopId, orderId);
        if (!OrderStatus.PENDING_RECEIPT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待收货的订单可以修改运单号");
        }
        long changed = orderLogMapper.selectCount(Wrappers.<OrderLog>lambdaQuery()
                .eq(OrderLog::getOrderId, orderId)
                .eq(OrderLog::getFromStatus, OrderStatus.PENDING_RECEIPT.name())
                .eq(OrderLog::getToStatus, OrderStatus.PENDING_RECEIPT.name()));
        if (changed >= 1) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "运单号只能修改一次");
        }
        order.setShipCompany(dto.shipCompany().trim());
        order.setShipNo(dto.shipNo().trim());
        saveOrder(order);
        writeLog(orderId, OrderStatus.PENDING_RECEIPT.name(), OrderStatus.PENDING_RECEIPT.name(),
                userId, OperatorType.SHOP, "修改运单号：" + order.getShipCompany() + " " + order.getShipNo());
        log.info("[Order] 修改运单号 orderId={}, no={}", orderId, order.getShipNo());
    }

    // ==================================================================
    // 定时任务
    // ==================================================================

    /** 每分钟：取消待付款超过 30 分钟的订单并释放库存 */
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .isNotNull(OrderInfo::getExpireTime)
                .lt(OrderInfo::getExpireTime, LocalDateTime.now())
                .orderByAsc(OrderInfo::getId));
        int count = 0;
        for (OrderInfo order : orders) {
            try {
                cancelInternal(order, "超时未支付", null, OperatorType.SYSTEM);
                count++;
            } catch (Exception e) {
                log.error("[Order] 超时取消订单失败 orderId={}: {}", order.getId(), e.getMessage());
            }
        }
        return count;
    }

    /** 每天：把发货满 7 天的待收货订单改成已完成 */
    @Transactional(rollbackFor = Exception.class)
    public int autoFinishOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(AUTO_FINISH_DAYS);
        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_RECEIPT.name())
                .isNotNull(OrderInfo::getShipTime)
                .le(OrderInfo::getShipTime, deadline)
                .orderByAsc(OrderInfo::getId));
        int count = 0;
        for (OrderInfo order : orders) {
            try {
                String from = order.getStatus();
                order.setStatus(OrderStatus.FINISHED.name());
                order.setFinishTime(LocalDateTime.now());
                saveOrder(order);
                writeLog(order.getId(), from, OrderStatus.FINISHED.name(), null, OperatorType.SYSTEM,
                        "发货满 " + AUTO_FINISH_DAYS + " 天自动确认收货");
                count++;
            } catch (Exception e) {
                log.error("[Order] 自动确认收货失败 orderId={}: {}", order.getId(), e.getMessage());
            }
        }
        return count;
    }

    // ==================================================================
    // 供售后模块复用的能力
    // ==================================================================

    /** 取订单（不存在抛 602） */
    public OrderInfo requireOrder(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在：" + orderId);
        }
        return order;
    }

    /** 取本店订单 */
    public OrderInfo requireShopOrder(Long shopId, Long orderId) {
        OrderInfo order = requireOrder(orderId);
        if (!shopId.equals(order.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在：" + orderId);
        }
        return order;
    }

    /** 保存订单（乐观锁） */
    public void saveOrder(OrderInfo order) {
        if (orderInfoMapper.updateById(order) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "订单已被他人修改，请刷新后重试");
        }
    }

    /** 写状态记录 */
    public void writeLog(Long orderId, String fromStatus, String toStatus, Long operatorId,
                         OperatorType operatorType, String remark) {
        OrderLog entity = new OrderLog();
        entity.setOrderId(orderId);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setOperatorId(operatorId);
        entity.setOperatorType(operatorType.name());
        entity.setRemark(remark);
        orderLogMapper.insert(entity);
    }

    // ==================================================================
    // 内部
    // ==================================================================

    /** 取消订单的公共实现：释放库存 + 退回优惠券 */
    private void cancelInternal(OrderInfo order, String reason, Long operatorId, OperatorType operatorType) {
        String from = order.getStatus();
        List<OrderItem> items = itemsOfOrder(order.getId());
        for (OrderItem item : items) {
            stockService.release(order.getShopId(), item.getSkuId(), item.getQuantity(),
                    order.getId(), operatorId);
        }
        couponService.returnByOrder(order.getId());

        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        saveOrder(order);
        writeLog(order.getId(), from, OrderStatus.CANCELLED.name(), operatorId, operatorType, reason);
        log.info("[Order] 取消订单 orderId={}, 原因={}", order.getId(), reason);
    }

    /** 下单行 */
    private record Line(ProductSku sku, Product product, int quantity, long price, String imageUrl) {
    }

    /** 计价构建结果 */
    private record BuildResult(List<Line> lines, List<CartItemVO> invalid, List<OrderItemVO> itemVos,
                               long goodsAmount, boolean priceChanged) {
    }

    /** 选中的优惠券 */
    private record CouponChoice(Long couponId, String name, long discount) {
    }

    /**
     * 把前端传来的行整理成可下单的订单行。
     * <p>失效行（下架 / 规格停用 / 库存不足）会进入 {@code invalid}；
     * {@code checkPrice} 为真时，与前端展示单价不一致会标记 {@code priceChanged}。</p>
     */
    private BuildResult build(Long shopId, List<OrderSkuDTO> items, boolean checkPrice) {
        // 同一 SKU 多行合并数量
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        Map<Long, Long> shownPriceMap = new HashMap<>();
        for (OrderSkuDTO item : items) {
            quantityMap.merge(item.skuId(), item.quantity(), Integer::sum);
            if (item.price() != null) {
                shownPriceMap.putIfAbsent(item.skuId(), item.price());
            }
        }

        List<Long> skuIds = new ArrayList<>(quantityMap.keySet());
        Map<Long, ProductSku> skuMap = new HashMap<>();
        for (ProductSku sku : skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .in(ProductSku::getId, skuIds))) {
            skuMap.put(sku.getId(), sku);
        }
        List<Long> productIds = skuMap.values().stream().map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product product : productMapper.selectList(Wrappers.<Product>lambdaQuery()
                    .in(Product::getId, productIds))) {
                productMap.put(product.getId(), product);
            }
        }
        Map<Long, List<String>> imageMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            imageMap.putAll(productAssembler.imagesOf(productIds));
        }

        List<Line> lines = new ArrayList<>();
        List<CartItemVO> invalid = new ArrayList<>();
        List<OrderItemVO> itemVos = new ArrayList<>();
        long goodsAmount = 0L;
        boolean priceChanged = false;

        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            Long skuId = entry.getKey();
            int quantity = entry.getValue();
            ProductSku sku = skuMap.get(skuId);
            Product product = sku == null ? null : productMap.get(sku.getProductId());
            List<String> images = product == null ? List.of()
                    : imageMap.getOrDefault(product.getId(), List.of());
            String cover = images.isEmpty() ? null : images.get(0);

            String reason = null;
            if (quantity > MAX_QUANTITY) {
                reason = "单次数量不能超过 " + MAX_QUANTITY + " 件";
            } else if (sku == null || !shopId.equals(sku.getShopId())) {
                reason = "商品不存在";
            } else if (product == null || !ProductStatus.ON_SALE.name().equals(product.getStatus())) {
                reason = "商品已下架";
            } else if (sku.getStatus() != null && sku.getStatus() != SKU_ENABLED) {
                reason = "规格已停用";
            } else if (sku.availableStock() <= 0) {
                reason = "库存不足";
            } else if (quantity > sku.availableStock()) {
                reason = "库存不足，仅剩 " + sku.availableStock() + " 件";
            }

            if (reason != null) {
                invalid.add(new CartItemVO(null, skuId, product == null ? null : product.getId(),
                        product == null ? null : product.getName(),
                        sku == null ? null : sku.getSpecName(), cover,
                        sku == null ? null : sku.getPrice(), quantity, null,
                        sku == null ? 0 : sku.availableStock(), false, reason));
                continue;
            }

            long price = sku.getPrice();
            Long shown = shownPriceMap.get(skuId);
            if (checkPrice && shown != null && shown != price) {
                priceChanged = true;
            }

            lines.add(new Line(sku, product, quantity, price, cover));
            itemVos.add(new OrderItemVO(null, null, product.getId(), sku.getId(), product.getName(),
                    sku.getSpecName(), cover, price, quantity, price * quantity));
            goodsAmount += price * quantity;
        }
        return new BuildResult(lines, invalid, itemVos, goodsAmount, priceChanged);
    }

    /** 校验优惠券可用并返回减免额 */
    private CouponChoice chooseCoupon(Long userId, Long shopId, Long couponUserId, long goodsAmount) {
        List<CouponUserVO> usable = couponService.usableCoupons(userId, shopId, goodsAmount);
        for (CouponUserVO coupon : usable) {
            if (coupon.id().equals(couponUserId)) {
                long reduce = coupon.reduceAmount() == null ? 0L : coupon.reduceAmount();
                return new CouponChoice(coupon.couponId(), coupon.name(), Math.min(reduce, goodsAmount));
            }
        }
        // 可用列表里找不到时，仍按规则报错（券过期、门槛不足等）
        couponService.lock(userId, shopId, couponUserId, goodsAmount, null);
        throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券不可用，请重新选择");
    }

    /**
     * 店主/店员不能在自己店铺下单。
     *
     * <p>返回 null 表示放行；返回文案时，预览页把它当提示显示，提交订单直接拒绝。</p>
     */
    private String selfPurchaseNotice(Shop shop, Long userId) {
        if (allowSelfPurchase) {
            return null;
        }
        ShopMember member = shopGuard.memberOf(shop.getId(), userId);
        if (member == null || (member.getStatus() != null && member.getStatus() == MEMBER_DISABLED)) {
            return null;
        }
        ShopRole role = ShopRole.of(member.getRole());
        String who = role == null ? "店铺成员" : role.getLabel();
        return "你是本店" + who + "，不能用自己账号在自己店铺下单，请换顾客账号购买";
    }

    private UserAddress requireOwnAddress(Long userId, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private boolean hasProcessingRefund(Long orderId) {
        return refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, orderId)
                .in(Refund::getStatus, RefundStatus.PENDING.name(), RefundStatus.AGREED.name(),
                        RefundStatus.WAITING_RETURN.name(), RefundStatus.RECEIVED.name())) > 0;
    }

    private List<OrderItem> itemsOfOrder(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private String generateOrderNo(Long shopId) {
        String prefix = LocalDateTime.now().format(ORDER_NO_FORMAT);
        for (int i = 0; i < 5; i++) {
            String no = prefix + String.format("%04d", RANDOM.nextInt(10_000));
            long exists = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                    .eq(OrderInfo::getShopId, shopId)
                    .eq(OrderInfo::getOrderNo, no));
            if (exists == 0) {
                return no;
            }
        }
        throw new ServiceException(ResultCode.ERROR, "订单号生成失败，请重试");
    }

    private List<OrderVO> toVoList(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(OrderInfo::getId).toList();
        Map<Long, List<OrderItem>> itemMap = new HashMap<>();
        for (OrderItem item : orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId))) {
            itemMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        Map<Long, Shop> shopMap = new HashMap<>();
        List<Long> shopIds = orders.stream().map(OrderInfo::getShopId).distinct().toList();
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
            shopMap.put(shop.getId(), shop);
        }
        Set<Long> reviewedOrderIds = new HashSet<>();
        for (Review review : reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
                .in(Review::getOrderId, orderIds))) {
            reviewedOrderIds.add(review.getOrderId());
        }

        List<OrderVO> rows = new ArrayList<>(orders.size());
        for (OrderInfo order : orders) {
            Shop shop = shopMap.get(order.getShopId());
            rows.add(toVo(order, shop == null ? null : shop.getName(),
                    itemMap.getOrDefault(order.getId(), List.of()),
                    reviewedOrderIds.contains(order.getId())));
        }
        return rows;
    }

    private OrderVO toVo(OrderInfo order, String shopName, List<OrderItem> items, boolean reviewed) {
        OrderStatus status = OrderStatus.of(order.getStatus());
        int itemCount = items.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
        String summary = null;
        if (!items.isEmpty()) {
            String first = items.get(0).getProductName();
            summary = items.size() == 1 ? first : first + " 等 " + items.size() + " 种商品";
        }
        String cover = items.isEmpty() ? null : items.get(0).getImageUrl();

        boolean unpaid = status == OrderStatus.PENDING_PAYMENT;
        boolean refunding = status == OrderStatus.REFUNDING || status == OrderStatus.REFUNDED;
        return new OrderVO(order.getId(), order.getOrderNo(), order.getShopId(), shopName,
                order.getStatus(), status == null ? order.getStatus() : status.getLabel(),
                orZero(order.getGoodsAmount()), orZero(order.getFreightAmount()),
                orZero(order.getDiscountAmount()), orZero(order.getPayAmount()),
                itemCount, summary, cover,
                StringUtils.maskMobile(order.getReceiverMobile()),
                order.getShipCompany(), order.getShipNo(), order.getCancelReason(),
                order.getCreateTime(), order.getPayTime(), order.getShipTime(),
                order.getFinishTime(), order.getCancelTime(), order.getExpireTime(),
                unpaid,
                status == OrderStatus.PENDING_RECEIPT,
                status == OrderStatus.FINISHED && !reviewed,
                !refunding && canApplyRefund(order, status),
                status == OrderStatus.PENDING_SHIPMENT && !refunding);
    }

    private boolean canApplyRefund(OrderInfo order, OrderStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case PENDING_SHIPMENT, PENDING_RECEIPT -> true;
            case FINISHED -> order.getFinishTime() != null
                    && order.getFinishTime().plusDays(REFUND_WINDOW_DAYS).isAfter(LocalDateTime.now());
            default -> false;
        };
    }

    private OrderDetailVO toDetailVo(OrderInfo order) {
        List<OrderItem> items = itemsOfOrder(order.getId());
        Shop shop = shopMapper.selectById(order.getShopId());
        boolean reviewed = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId())) > 0;
        OrderVO summary = toVo(order, shop == null ? null : shop.getName(), items, reviewed);

        List<OrderItemVO> itemVos = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            itemVos.add(new OrderItemVO(item.getId(), item.getOrderId(), item.getProductId(),
                    item.getSkuId(), item.getProductName(), item.getSpecName(), item.getImageUrl(),
                    item.getPrice(), item.getQuantity(), item.getAmount()));
        }

        List<OrderLog> logs = orderLogMapper.selectList(Wrappers.<OrderLog>lambdaQuery()
                .eq(OrderLog::getOrderId, order.getId())
                .orderByAsc(OrderLog::getCreateTime)
                .orderByAsc(OrderLog::getId));
        List<OrderLogVO> logVos = new ArrayList<>(logs.size());
        for (OrderLog entity : logs) {
            OrderStatus from = OrderStatus.of(entity.getFromStatus());
            OrderStatus to = OrderStatus.of(entity.getToStatus());
            OperatorType operatorType = operatorTypeOf(entity.getOperatorType());
            logVos.add(new OrderLogVO(entity.getId(), entity.getFromStatus(),
                    from == null ? entity.getFromStatus() : from.getLabel(),
                    entity.getToStatus(), to == null ? entity.getToStatus() : to.getLabel(),
                    entity.getOperatorType(),
                    operatorType == null ? entity.getOperatorType() : operatorType.getLabel(),
                    entity.getOperatorId(), entity.getRemark(), entity.getCreateTime()));
        }

        Refund refund = refundMapper.selectOne(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, order.getId())
                .orderByDesc(Refund::getId)
                .last("LIMIT 1"));

        return new OrderDetailVO(summary, order.getReceiverName(), order.getReceiverMobile(),
                order.getReceiverAddress(), order.getBuyerRemark(), payChannelLabel(order.getPayChannel()),
                order.getPayNo(), order.getPaidAmount(), itemVos, logVos,
                RefundVos.toVo(refund, order.getOrderNo(), shop == null ? null : shop.getName()));
    }

    /** 收款渠道编码转中文，前端直接展示，避免出现 OFFLINE 这种原始值 */
    private String payChannelLabel(String code) {
        PayChannel channel = PayChannel.of(code);
        return channel == null ? code : channel.getLabel();
    }

    private OperatorType operatorTypeOf(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        try {
            return OperatorType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private LocalDateTime parseStartOfDay(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(date.trim()).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseEndOfDay(String date) {
        LocalDateTime start = parseStartOfDay(date);
        return start == null ? null : start.plusDays(1).minusNanos(1);
    }
}
