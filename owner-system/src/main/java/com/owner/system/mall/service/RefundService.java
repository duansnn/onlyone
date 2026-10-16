package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.RefundApplyDTO;
import com.owner.system.mall.domain.dto.RefundRefuseDTO;
import com.owner.system.mall.domain.dto.RefundReturnShipDTO;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.OrderLog;
import com.owner.system.mall.domain.entity.Payment;
import com.owner.system.mall.domain.entity.Refund;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.enums.OperatorType;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.PayStatus;
import com.owner.system.mall.domain.enums.RefundStatus;
import com.owner.system.mall.domain.enums.RefundType;
import com.owner.system.mall.domain.vo.RefundVO;
import com.owner.system.mall.mapper.OrderInfoMapper;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.OrderLogMapper;
import com.owner.system.mall.mapper.PaymentMapper;
import com.owner.system.mall.mapper.RefundMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PaymentGateway;
import com.owner.system.mall.support.RefundVos;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 售后服务（PRD §9）。
 *
 * <p><b>整单售后，不做部分退款。</b>一笔订单同一时刻只允许一条进行中的售后单；售后进行中时订单主状态是
 * 「退款中」，此时不能发货、不能评价。</p>
 *
 * <p>两条路径：</p>
 * <ul>
 *   <li><b>未发货 · 仅退款</b>：待发货状态下申请 → 店里同意即原路退款 + 库存退回 → 订单已退款；</li>
 *   <li><b>已发货 / 已完成 7 天内 · 退货退款</b>：店里同意并给出退货收件信息（默认用店铺发货地址）→
 *       顾客填退货运单号 → 店里确认收货 → 再发起退款。</li>
 * </ul>
 *
 * <p>拒绝时必须填理由，订单恢复到申请前的状态（申请时的原状态记在 {@code order_log} 里）。</p>
 *
 * <p><b>关于退款本身</b>：退款必须经 {@link PaymentGateway} 真正发起。通道未开通时
 * 直接失败并回滚，订单仍停在「退款中」，不会出现「钱没退却显示已退款」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    /** 未发货「仅退款」可选原因 */
    public static final List<String> REFUND_ONLY_REASONS = List.of("不想要了", "拍错", "地址错误", "其他");

    /** 已发货「退货退款」可选原因 */
    public static final List<String> RETURN_REFUND_REASONS = List.of(
            "不想要了", "商品与描述不符", "质量问题", "少件或漏发", "其他");

    /** 后台售后列表的分组筛选项 */
    public static final List<String> STATUS_FILTERS = List.of("PENDING", "AGREED", "REJECTED", "SUCCESS");

    private final RefundMapper refundMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final PaymentMapper paymentMapper;
    private final ShopMapper shopMapper;
    private final OrderService orderService;
    private final StockService stockService;
    private final ShopGuard shopGuard;
    private final PaymentGateway paymentGateway;

    // ==================================================================
    // 顾客侧
    // ==================================================================

    /**
     * 申请售后。
     * <p>待发货 → 只能「仅退款」；待收货 / 已完成 7 天内 → 只能「退货退款」。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long orderId, RefundApplyDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = orderService.requireOrder(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }

        RefundType type = RefundType.of(dto.type());
        if (type == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "售后类型只能是 REFUND_ONLY 或 RETURN_REFUND");
        }

        Refund last = latestOfOrder(orderId);
        if (last != null) {
            RefundStatus lastStatus = RefundStatus.of(last.getStatus());
            if (lastStatus != null && !lastStatus.isEnd()) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR, "这笔订单还有未处理完的售后，请等店里处理");
            }
        }

        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "订单状态异常，无法申请售后");
        }
        if (type == RefundType.REFUND_ONLY) {
            if (status != OrderStatus.PENDING_SHIPMENT) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR,
                        "只有待发货的订单可以申请仅退款；已发货请选退货退款");
            }
        } else {
            if (status == OrderStatus.PENDING_RECEIPT) {
                // 已发货，随时可申请
            } else if (status == OrderStatus.FINISHED) {
                if (order.getFinishTime() == null
                        || !order.getFinishTime().plusDays(OrderService.REFUND_WINDOW_DAYS)
                        .isAfter(LocalDateTime.now())) {
                    throw new ServiceException(ResultCode.BUSINESS_ERROR,
                            "已超过确认收货 " + OrderService.REFUND_WINDOW_DAYS + " 天，不能再申请售后");
                }
            } else {
                throw new ServiceException(ResultCode.BUSINESS_ERROR,
                        "当前订单状态不能申请退货退款（" + status.getLabel() + "）");
            }
        }

        Payment payment = successPaymentOf(orderId);
        if (payment == null) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "这笔订单没有成功的支付记录，无法申请退款");
        }

        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setShopId(order.getShopId());
        refund.setUserId(userId);
        refund.setType(type.name());
        refund.setReason(dto.reason().trim());
        refund.setDescription(StringUtils.trimToNull(dto.description()));
        refund.setStatus(RefundStatus.PENDING.name());
        // 整单退款，金额取实付
        refund.setRefundAmount(orZero(order.getPaidAmount()) > 0
                ? order.getPaidAmount() : orZero(order.getPayAmount()));
        refund.setVersion(0);
        refundMapper.insert(refund);

        String from = order.getStatus();
        order.setStatus(OrderStatus.REFUNDING.name());
        orderService.saveOrder(order);
        orderService.writeLog(orderId, from, OrderStatus.REFUNDING.name(), userId,
                OperatorType.CUSTOMER, "申请售后：" + type.getLabel() + "（" + refund.getReason() + "）");

        log.info("[Refund] 顾客申请售后 refundId={}, orderId={}, type={}, amount={}分",
                refund.getId(), orderId, type, refund.getRefundAmount());
        return refund.getId();
    }

    /** 我的售后列表 */
    public PageResult<RefundVO> myRefunds(String status, PageQuery query) {
        Long userId = MallCurrentUser.requireUserId();
        LambdaQueryWrapper<Refund> wrapper = Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getUserId, userId);
        applyStatusFilter(wrapper, status);
        wrapper.orderByDesc(Refund::getCreateTime).orderByDesc(Refund::getId);
        Page<Refund> page = refundMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()), wrapper);
        return PageResult.of(toVoList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 我的售后详情。用别人的售后编号访问返回不存在。 */
    public RefundVO myRefundDetail(Long refundId) {
        Long userId = MallCurrentUser.requireUserId();
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !userId.equals(refund.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "售后单不存在");
        }
        return toVo(refund);
    }

    /** 顾客填写退货运单号 */
    @Transactional(rollbackFor = Exception.class)
    public void submitReturnShip(Long refundId, RefundReturnShipDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !userId.equals(refund.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "售后单不存在");
        }
        RefundStatus status = RefundStatus.of(refund.getStatus());
        if (status != RefundStatus.WAITING_RETURN) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    status == RefundStatus.RECEIVED || status == RefundStatus.SUCCESS
                            ? "退货运单号已经提交过了"
                            : "店里还没有同意退货，暂时不能填单号");
        }
        refund.setReturnCompany(dto.returnCompany().trim());
        refund.setReturnShipNo(dto.returnShipNo().trim());
        refund.setReturnTime(LocalDateTime.now());
        updateRefund(refund);

        orderService.writeLog(refund.getOrderId(), OrderStatus.REFUNDING.name(), OrderStatus.REFUNDING.name(),
                userId, OperatorType.CUSTOMER,
                "顾客已寄回：" + refund.getReturnCompany() + " " + refund.getReturnShipNo());
        log.info("[Refund] 顾客填退货运单 refundId={}, company={}, no={}",
                refundId, refund.getReturnCompany(), refund.getReturnShipNo());
    }

    // ==================================================================
    // 店铺后台
    // ==================================================================

    /**
     * 后台售后分页。
     *
     * @param status 分组筛选：PENDING 待处理 / AGREED 已同意（含待退货、已收货）/ REJECTED 已拒绝 / SUCCESS 已完成
     */
    public PageResult<RefundVO> shopRefunds(Long shopId, String status, PageQuery query) {
        shopGuard.requireMember(shopId);
        LambdaQueryWrapper<Refund> wrapper = Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getShopId, shopId);
        applyStatusFilter(wrapper, status);
        wrapper.orderByDesc(Refund::getCreateTime).orderByDesc(Refund::getId);
        Page<Refund> page = refundMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()), wrapper);
        return PageResult.of(toVoList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 后台售后详情 */
    public RefundVO shopRefundDetail(Long shopId, Long refundId) {
        shopGuard.requireMember(shopId);
        return toVo(requireShopRefund(shopId, refundId));
    }

    /**
     * 同意售后。
     * <p>仅退款：立刻原路退款并退回库存，订单变已退款。<br>
     * 退货退款：给出退货收件信息（默认店铺发货地址），等顾客寄回。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void agree(Long shopId, Long refundId) {
        shopGuard.requireMember(shopId);
        Long operatorId = MallCurrentUser.requireUserId();
        Refund refund = requireShopRefund(shopId, refundId);

        if (RefundStatus.of(refund.getStatus()) != RefundStatus.PENDING) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待处理的售后可以同意");
        }
        OrderInfo order = orderService.requireOrder(refund.getOrderId());
        RefundType type = RefundType.of(refund.getType());

        if (type == RefundType.RETURN_REFUND) {
            Shop shop = shopGuard.loadShop(shopId);
            String returnAddress = StringUtils.trimToNull(shop.getShipAddress());
            if (returnAddress == null) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR,
                        "店铺还没有填发货地址，请先在店铺资料里补充，顾客要按这个地址寄回");
            }
            refund.setStatus(RefundStatus.WAITING_RETURN.name());
            refund.setReturnAddress(returnAddress);
            updateRefund(refund);
            orderService.writeLog(order.getId(), OrderStatus.REFUNDING.name(), OrderStatus.REFUNDING.name(),
                    operatorId, OperatorType.SHOP,
                    "售后同意退货退款，请顾客寄回：" + returnAddress);
            log.info("[Refund] 同意退货退款 refundId={}, 退货地址={}", refundId, returnAddress);
            return;
        }

        // 仅退款：同意即退款
        doRefund(refund, order, operatorId, "同意仅退款");
    }

    /** 拒绝售后：必须填理由，订单回到申请前的状态 */
    @Transactional(rollbackFor = Exception.class)
    public void refuse(Long shopId, Long refundId, RefundRefuseDTO dto) {
        shopGuard.requireMember(shopId);
        Long operatorId = MallCurrentUser.requireUserId();
        Refund refund = requireShopRefund(shopId, refundId);

        RefundStatus status = RefundStatus.of(refund.getStatus());
        if (status == null || status.isEnd()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "这笔售后已经结束了，不能再拒绝");
        }
        OrderInfo order = orderService.requireOrder(refund.getOrderId());

        String back = previousOrderStatus(order.getId());
        if (OrderStatus.of(back) == null || OrderStatus.REFUNDING.name().equals(back)) {
            // 兜底：按售后再推一次，避免订单卡在退款中
            back = RefundType.of(refund.getType()) == RefundType.REFUND_ONLY
                    ? OrderStatus.PENDING_SHIPMENT.name()
                    : (order.getFinishTime() != null
                    ? OrderStatus.FINISHED.name() : OrderStatus.PENDING_RECEIPT.name());
        }

        refund.setStatus(RefundStatus.REJECTED.name());
        refund.setRefuseReason(dto.refuseReason().trim());
        refund.setFinishTime(LocalDateTime.now());
        updateRefund(refund);

        String from = order.getStatus();
        order.setStatus(back);
        orderService.saveOrder(order);
        orderService.writeLog(order.getId(), from, back, operatorId, OperatorType.SHOP,
                "售后被拒：" + refund.getRefuseReason());

        log.info("[Refund] 拒绝售后 refundId={}, 理由={}, 订单回到={}", refundId, refund.getRefuseReason(), back);
    }

    /** 确认收到退货，随后发起退款 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceived(Long shopId, Long refundId) {
        shopGuard.requireMember(shopId);
        Long operatorId = MallCurrentUser.requireUserId();
        Refund refund = requireShopRefund(shopId, refundId);

        if (RefundStatus.of(refund.getStatus()) != RefundStatus.WAITING_RETURN) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "这笔售后当前不是「待退货」状态");
        }
        if (StringUtils.isBlank(refund.getReturnShipNo())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "顾客还没有填写退货运单号，先别确认收货");
        }
        OrderInfo order = orderService.requireOrder(refund.getOrderId());

        refund.setStatus(RefundStatus.RECEIVED.name());
        updateRefund(refund);
        orderService.writeLog(order.getId(), OrderStatus.REFUNDING.name(), OrderStatus.REFUNDING.name(),
                operatorId, OperatorType.SHOP,
                "已收到退货：" + refund.getReturnCompany() + " " + refund.getReturnShipNo());

        doRefund(refund, order, operatorId, "已收到退货");
    }

    // ==================================================================
    // 内部
    // ==================================================================

    /**
     * 真正的退款：原路退回 → 库存退回 → 订单已退款。
     *
     * <p>退款通道没开通时直接抛异常并回滚，订单留在「退款中」，绝不假装退款成功。</p>
     */
    private void doRefund(Refund refund, OrderInfo order, Long operatorId, String remark) {
        Payment payment = successPaymentOf(order.getId());
        if (payment == null) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "找不到该订单已入账的支付记录，无法退款");
        }
        long amount = orZero(refund.getRefundAmount());
        if (amount <= 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "退款金额异常，无法退款");
        }
        if (!paymentGateway.refund(payment, amount, refund.getReason())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "退款通道未开通，退款没有发起，订单仍停在退款中");
        }

        // 库存退回（仅退款是货没发出去、退货退款是货回来了，两种都要加回库存）
        for (OrderItem item : itemsOfOrder(order.getId())) {
            stockService.giveBack(order.getShopId(), item.getSkuId(), item.getQuantity(),
                    order.getId(), operatorId);
        }

        refund.setStatus(RefundStatus.SUCCESS.name());
        refund.setFinishTime(LocalDateTime.now());
        updateRefund(refund);

        String from = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED.name());
        orderService.saveOrder(order);
        orderService.writeLog(order.getId(), from, OrderStatus.REFUNDED.name(), operatorId,
                OperatorType.SHOP, "退款 " + amount + " 分原路退回（" + remark + "）");

        log.info("[Refund] 退款完成 refundId={}, orderId={}, amount={}分",
                refund.getId(), order.getId(), amount);
    }

    /** 后台筛选分组 → 具体状态 */
    private void applyStatusFilter(LambdaQueryWrapper<Refund> wrapper, String status) {
        if (StringUtils.isBlank(status) || "ALL".equalsIgnoreCase(status.trim())) {
            return;
        }
        String normalized = status.trim().toUpperCase();
        switch (normalized) {
            case "PENDING" -> wrapper.eq(Refund::getStatus, RefundStatus.PENDING.name());
            case "AGREED" -> wrapper.in(Refund::getStatus, RefundStatus.AGREED.name(),
                    RefundStatus.WAITING_RETURN.name(), RefundStatus.RECEIVED.name());
            case "REJECTED" -> wrapper.eq(Refund::getStatus, RefundStatus.REJECTED.name());
            case "SUCCESS", "FINISHED" -> wrapper.eq(Refund::getStatus, RefundStatus.SUCCESS.name());
            default -> {
                // 也允许直接传细分的状态码
                RefundStatus exact = RefundStatus.of(normalized);
                if (exact != null) {
                    wrapper.eq(Refund::getStatus, exact.name());
                }
            }
        }
    }

    /** 申请售后时记录的订单原状态（从 order_log 反查，不用额外加字段） */
    private String previousOrderStatus(Long orderId) {
        OrderLog log = orderLogMapper.selectOne(Wrappers.<OrderLog>lambdaQuery()
                .eq(OrderLog::getOrderId, orderId)
                .eq(OrderLog::getToStatus, OrderStatus.REFUNDING.name())
                .orderByDesc(OrderLog::getId)
                .last("LIMIT 1"));
        return log == null ? null : log.getFromStatus();
    }

    private Refund requireShopRefund(Long shopId, Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !shopId.equals(refund.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "售后单不存在：" + refundId);
        }
        return refund;
    }

    private Refund latestOfOrder(Long orderId) {
        return refundMapper.selectOne(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, orderId)
                .orderByDesc(Refund::getId)
                .last("LIMIT 1"));
    }

    /** 该订单已入账的支付单 */
    private Payment successPaymentOf(Long orderId) {
        return paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, PayStatus.SUCCESS.name())
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
    }

    private void updateRefund(Refund refund) {
        if (refundMapper.updateById(refund) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "售后单已被他人修改，请刷新后重试");
        }
    }

    private List<OrderItem> itemsOfOrder(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private RefundVO toVo(Refund refund) {
        List<RefundVO> rows = toVoList(List.of(refund));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<RefundVO> toVoList(List<Refund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, OrderInfo> orderMap = new HashMap<>();
        List<Long> orderIds = refunds.stream().map(Refund::getOrderId).distinct().toList();
        if (!orderIds.isEmpty()) {
            for (OrderInfo order : orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                    .in(OrderInfo::getId, orderIds))) {
                orderMap.put(order.getId(), order);
            }
        }
        Map<Long, String> shopNames = new HashMap<>();
        List<Long> shopIds = refunds.stream().map(Refund::getShopId).distinct().toList();
        if (!shopIds.isEmpty()) {
            for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
                shopNames.put(shop.getId(), shop.getName());
            }
        }

        List<RefundVO> rows = new ArrayList<>(refunds.size());
        for (Refund refund : refunds) {
            OrderInfo order = orderMap.get(refund.getOrderId());
            rows.add(RefundVos.toVo(refund, order == null ? null : order.getOrderNo(),
                    shopNames.get(refund.getShopId())));
        }
        return rows;
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }
}
