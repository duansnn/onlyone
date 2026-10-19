package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.Payment;
import com.owner.system.mall.domain.enums.OperatorType;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.PayChannel;
import com.owner.system.mall.domain.enums.PayStatus;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.PaymentMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收款与退款记账服务。
 *
 * <p><b>本项目不接入任何在线支付通道</b>（没有微信支付的 SDK / 商户号 / 证书，也没有支付宝），
 * 顾客端没有任何支付入口。交易按「线下收款」处理：</p>
 *
 * <ul>
 *   <li>下单 → 订单停在 {@code PENDING_PAYMENT}（待确认），库存锁定、优惠券预占；</li>
 *   <li>商家线下收到钱（现金 / 转账 / 到付），在店铺后台点「确认收款」→ 本类的
 *       {@link #markPaidOffline} 把订单转 {@code PENDING_SHIPMENT}，锁定库存转扣减、优惠券核销；</li>
 *   <li>退款同理，由商家线下退回后点「同意退款」，见 {@code RefundService}。</li>
 * </ul>
 *
 * <p>系统不会伪造任何「支付成功」，也没有「我已付款」按钮 —— 钱到没到只由商家确认。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentMapper paymentMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final StockService stockService;
    private final CouponService couponService;
    private final ShopGuard shopGuard;

    /**
     * 商家后台确认收到钱（线下收款：现金 / 转账 / 到付）。
     *
     * <p>没有任何在线通道参与：订单是否收到钱由商家在店铺后台确认。记账效果与真实支付成功一致 ——
     * 写一条成功支付单（渠道 {@code OFFLINE}）、锁定库存转扣减、优惠券核销、订单转待发货，并留一条状态记录。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPaidOffline(Long shopId, Long orderId) {
        shopGuard.requireMember(shopId);
        Long operatorId = MallCurrentUser.requireUserId();
        OrderInfo order = orderService.requireOrder(orderId);
        if (!shopId.equals(order.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有待确认的订单可以确认收款");
        }

        long amount = order.getPayAmount() == null ? 0L : order.getPayAmount();
        LocalDateTime now = LocalDateTime.now();
        String platformNo = "OFFLINE" + System.currentTimeMillis();

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setShopId(shopId);
        payment.setChannel(PayChannel.OFFLINE.name());
        payment.setAmount(amount);
        payment.setOutTradeNo(generateOutTradeNo(order.getOrderNo()));
        payment.setPlatformTradeNo(platformNo);
        payment.setStatus(PayStatus.SUCCESS.name());
        payment.setNotifyBody("线下收款，由店铺后台确认，操作人 " + operatorId);
        paymentMapper.insert(payment);

        String from = order.getStatus();
        order.setStatus(OrderStatus.PENDING_SHIPMENT.name());
        order.setPayChannel(PayChannel.OFFLINE.name());
        order.setPayNo(platformNo);
        order.setPayTime(now);
        order.setPaidAmount(amount);
        orderService.saveOrder(order);

        // 锁定转扣减
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            stockService.confirmPaid(shopId, item.getSkuId(), item.getQuantity(), orderId, operatorId);
        }
        couponService.useByOrder(orderId);
        orderService.writeLog(orderId, from, OrderStatus.PENDING_SHIPMENT.name(), operatorId,
                OperatorType.SHOP, "商家确认收款（线下收款 " + amount / 100.0 + " 元）");

        log.info("[Pay] 线下收款确认 orderNo={}, amount={}分, 操作人={}",
                order.getOrderNo(), amount, operatorId);
    }

    private String generateOutTradeNo(String orderNo) {
        return orderNo + "-" + String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
