package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台「顾客管理」列表行。
 *
 * <p>单商家模式下，店主需要一眼看清「这个人是谁、下过几单、花了多少钱、最近什么时候来的」，
 * 所以这里把手机号给全号（要发货、要联系），并带上消费统计。</p>
 */
@Schema(description = "顾客管理-列表行")
public record PlatformUserVO(

        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "登录账号")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "手机号")
        String mobile,

        @Schema(description = "邮箱")
        String email,

        @Schema(description = "是否店主（店主不进顾客列表，仅详情兜底用）")
        boolean merchant,

        @Schema(description = "状态：0 正常，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "是否已设置密码")
        boolean passwordSet,

        @Schema(description = "订单数（不含已取消）")
        long orderCount,

        @Schema(description = "累计消费（分，只算已收款且未退款的订单）")
        long paidAmount,

        @Schema(description = "最后下单时间，从未下单为 null")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime lastOrderTime,

        @Schema(description = "收货地址数")
        long addressCount,

        @Schema(description = "注册时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
