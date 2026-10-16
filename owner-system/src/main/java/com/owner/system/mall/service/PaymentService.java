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
import com.owner.system.mall.domain.vo.PaymentCreateVO;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.PaymentMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PaymentGateway;
import com.owner.system.mall.support.PaymentNotify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付服务。
 *
 * <p>金额由服务端从订单读取，不接受前端传入；以支付平台的异步通知为准入账；
 * 同一订单重复通知只入账一次；金额不一致、订单已取消、订单已支付都会被拒绝并记录原因。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentMapper paymentMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;
    private final StockService stockService;
    private final CouponService couponService;

    /** 顾客在待付款订单发起支付 */
    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateVO create(Long orderId, String channelCode) {
        Long userId = MallCurrentUser.requireUserId();
        PayChannel channel = PayChannel.of(channelCode);
        if (channel == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "支付方式只能是 WECHAT 或 ALIPAY");
        }
        OrderInfo order = orderService.requireOrder(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "订单当前状态不能支付");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "订单已超时，请重新下单");
        }

        long amount = order.getPayAmount() == null ? 0L : order.getPayAmount();
        if (!paymentGateway.isEnabled(channel)) {
            log.info("[Pay] 订单 {} 发起 {} 支付，但通道未开通", order.getOrderNo(), channel);
            return new PaymentCreateVO(order.getId(), order.getOrderNo(), amount,
                    channel.name(), channel.getLabel(), false, null, null,
                    "支付未开通，订单已保留在待付款");
        }

        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getChannel, channel.name())
                .eq(Payment::getStatus, PayStatus.PENDING.name())
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
            payment.setShopId(order.getShopId());
            payment.setChannel(channel.name());
            payment.setAmount(amount);
            payment.setOutTradeNo(generateOutTradeNo(order.getOrderNo()));
            payment.setStatus(PayStatus.PENDING.name());
            paymentMapper.insert(payment);
        }
        String payload = paymentGateway.prepay(payment, "订单 " + order.getOrderNo());
        return new PaymentCreateVO(order.getId(), order.getOrderNo(), amount, channel.name(),
                channel.getLabel(), true, payment.getOutTradeNo(), payload, null);
    }

    /**
     * 处理支付平台的异步通知。
     *
     * @return true 表示已受理（平台可以停止重试）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleNotify(PayChannel channel, String body) {
        PaymentNotify notify = paymentGateway.parseNotify(channel, body);
        if (notify == null) {
            log.warn("[Pay] {} 通知验签或解析失败，已忽略", channel);
            return false;
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOutTradeNo, notify.outTradeNo())
                .last("LIMIT 1"));
        if (payment == null) {
            log.warn("[Pay] 通知的商户单号不存在：{}", notify.outTradeNo());
            return false;
        }

        // 幂等：已成功的支付单不再入账
        if (PayStatus.SUCCESS.name().equals(payment.getStatus())) {
            payment.setNotifyBody(notify.rawBody());
            paymentMapper.updateById(payment);
            log.info("[Pay] 重复通知，已忽略 outTradeNo={}", notify.outTradeNo());
            return true;
        }

        OrderInfo order = orderService.requireOrder(payment.getOrderId());
        long orderPayAmount = order.getPayAmount() == null ? 0L : order.getPayAmount();

        String reject = null;
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            reject = "订单已取消";
        } else if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            reject = "订单不是待付款状态：" + order.getStatus();
        } else if (!notify.success()) {
            reject = "平台判定支付失败";
        } else if (notify.amount() != orderPayAmount) {
            reject = "金额不一致：通知 " + notify.amount() + " 分，订单应付 " + orderPayAmount + " 分";
        }

        if (reject != null) {
            payment.setStatus(PayStatus.FAILED.name());
            payment.setFailReason(reject);
            payment.setNotifyBody(notify.rawBody());
            paymentMapper.updateById(payment);
            log.warn("[Pay] 拒绝入账 outTradeNo={}，原因：{}", notify.outTradeNo(), reject);
            return false;
        }

        payment.setStatus(PayStatus.SUCCESS.name());
        payment.setPlatformTradeNo(notify.platformTradeNo());
        payment.setNotifyBody(notify.rawBody());
        paymentMapper.update(payment, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, payment.getId())
                .set(Payment::getFailReason, null));

        String from = order.getStatus();
        order.setStatus(OrderStatus.PENDING_SHIPMENT.name());
        order.setPayChannel(channel.name());
        order.setPayNo(notify.platformTradeNo());
        order.setPayTime(LocalDateTime.now());
        order.setPaidAmount(notify.amount());
        orderService.saveOrder(order);

        // 锁定转扣减
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            stockService.confirmPaid(order.getShopId(), item.getSkuId(), item.getQuantity(),
                    order.getId(), null);
        }
        couponService.useByOrder(order.getId());
        orderService.writeLog(order.getId(), from, OrderStatus.PENDING_SHIPMENT.name(), null,
                OperatorType.SYSTEM, "支付成功（" + channel.getLabel() + "）");

        log.info("[Pay] 入账成功 orderNo={}, outTradeNo={}, amount={}分",
                order.getOrderNo(), notify.outTradeNo(), notify.amount());
        return true;
    }

    private String generateOutTradeNo(String orderNo) {
        return orderNo + "-" + String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
