package com.owner.system.mall.support;

import com.owner.system.mall.domain.entity.Refund;
import com.owner.system.mall.domain.enums.RefundStatus;
import com.owner.system.mall.domain.enums.RefundType;
import com.owner.system.mall.domain.vo.RefundVO;

/**
 * 售后出参组装（静态工具，避免 OrderService 与 RefundService 互相注入形成循环依赖）。
 */
public final class RefundVos {

    private RefundVos() {
    }

    public static RefundVO toVo(Refund refund, String orderNo, String shopName) {
        if (refund == null) {
            return null;
        }
        RefundType type = RefundType.of(refund.getType());
        RefundStatus status = RefundStatus.of(refund.getStatus());
        return new RefundVO(refund.getId(), refund.getOrderId(), orderNo, refund.getShopId(),
                shopName, refund.getUserId(), refund.getType(),
                type == null ? refund.getType() : type.getLabel(),
                refund.getReason(), refund.getDescription(), refund.getStatus(),
                status == null ? refund.getStatus() : status.getLabel(),
                refund.getRefundAmount(), refund.getRefuseReason(), refund.getReturnCompany(),
                refund.getReturnShipNo(), refund.getReturnAddress(), refund.getReturnTime(),
                refund.getFinishTime(), refund.getCreateTime());
    }
}
