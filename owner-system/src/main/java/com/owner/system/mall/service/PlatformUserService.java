package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.entity.UserAddress;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.vo.AddressVO;
import com.owner.system.mall.domain.vo.CustomerDetailVO;
import com.owner.system.mall.domain.vo.CustomerOrderVO;
import com.owner.system.mall.domain.vo.PlatformUserVO;
import com.owner.system.mall.mapper.OrderInfoMapper;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.ShopMemberMapper;
import com.owner.system.mall.mapper.UserAddressMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PasswordService;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顾客管理（单商家模式）。
 *
 * <p>店主能看全部注册顾客的档案、消费统计、收货地址与订单，并能停用账号、重置密码。
 * 这里<b>不负责</b>创建账号——顾客自己在店面的登录页注册；店主也不该替顾客编密码。</p>
 *
 * <p>权限：所有方法先过 {@link MallCurrentUser#requireMerchant()}，
 * 顾客拿自己的令牌调这些接口一律 403。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformUserService {

    private static final int USER_ENABLED = 0;
    private static final int USER_DISABLED = 1;

    /** 详情里最多回多少条订单 */
    private static final int RECENT_ORDER_LIMIT = 20;

    /** 已取消的订单不计入"下过几单" */
    private static final String STATUS_CANCELLED = OrderStatus.CANCELLED.name();

    private final SysUserMapper sysUserMapper;
    private final PasswordService passwordService;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserAddressMapper userAddressMapper;
    private final ShopMemberMapper shopMemberMapper;

    /** 顾客分页：按账号 / 昵称 / 手机号模糊查，带消费统计 */
    public PageResult<PlatformUserVO> page(String keyword, Integer status, PageQuery query) {
        MallCurrentUser.requireMerchant();

        // 店铺成员（店主/店员）不是「顾客」，不进这张列表
        List<Long> staffIds = shopMemberMapper.selectList(Wrappers.<ShopMember>lambdaQuery()
                        .select(ShopMember::getUserId))
                .stream().map(ShopMember::getUserId).distinct().toList();

        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getPlatformAdmin, 0)
                        .notIn(!staffIds.isEmpty(), SysUser::getId, staffIds)
                        .eq(status != null, SysUser::getStatus, status)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getMobile, keyword))
                        .orderByDesc(SysUser::getCreateTime)
                        .orderByDesc(SysUser::getId));

        List<SysUser> users = page.getRecords();
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, OrderStat> stats = orderStats(userIds);
        Map<Long, Long> addressCounts = addressCounts(userIds);

        List<PlatformUserVO> rows = new ArrayList<>(users.size());
        for (SysUser user : users) {
            rows.add(toVo(user, stats.get(user.getId()), addressCounts.getOrDefault(user.getId(), 0L)));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 顾客详情：档案 + 统计 + 地址 + 最近订单 */
    public CustomerDetailVO detail(Long userId) {
        MallCurrentUser.requireMerchant();
        SysUser user = requireUser(userId);

        List<UserAddress> addresses = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));

        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getUserId, userId)
                .orderByDesc(OrderInfo::getCreateTime)
                .orderByDesc(OrderInfo::getId)
                .last("LIMIT " + RECENT_ORDER_LIMIT));

        // 统计用全量口径，不能用上面这个「最近 20 条」
        OrderStat stat = orderStats(List.of(userId)).get(userId);

        List<AddressVO> addressRows = new ArrayList<>(addresses.size());
        for (UserAddress address : addresses) {
            addressRows.add(new AddressVO(address.getId(), address.getReceiver(), address.getMobile(),
                    address.getProvince(), address.getCity(), address.getDistrict(), address.getDetail(),
                    AddressVO.join(address.getProvince(), address.getCity(), address.getDistrict(),
                            address.getDetail()),
                    address.getIsDefault()));
        }

        return new CustomerDetailVO(
                toVo(user, stat, addressRows.size()),
                addressRows,
                toCustomerOrders(orders));
    }

    /** 停用 / 启用账号。停用后不能登录；不能停用自己。 */
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long userId, int status) {
        Long operatorId = MallCurrentUser.requireUserId();
        MallCurrentUser.requireMerchant();
        if (status != USER_ENABLED && status != USER_DISABLED) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        if (status == USER_DISABLED && operatorId.equals(userId)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "不能停用自己当前的账号");
        }
        SysUser user = requireUser(userId);
        if (user.getPlatformAdmin() != null && user.getPlatformAdmin() == 1) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "店主账号不能在这里停用");
        }
        if (user.getStatus() != null && user.getStatus() == status) {
            return;
        }
        user.setStatus(status);
        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Customer] userId={} 状态改为 {}", userId, status == USER_DISABLED ? "停用" : "正常");
    }

    /** 重置密码。只写新的 BCrypt，不返回明文。 */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        MallCurrentUser.requireMerchant();
        SysUser user = requireUser(userId);
        user.setPasswordHash(passwordService.encode(newPassword));
        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Customer] 已重置 userId={} 的密码", userId);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /**
     * 按用户聚合订单：单量、累计消费、最后下单时间。
     * <p>用户量小（一页 10 个），直接把这批人的订单捞回来在内存里算，比写一堆
     * {@code GROUP BY} 好维护。已取消的单不算单量，也不算消费。</p>
     */
    private Map<Long, OrderStat> orderStats(Collection<Long> userIds) {
        Map<Long, OrderStat> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .in(OrderInfo::getUserId, userIds)
                .select(OrderInfo::getUserId, OrderInfo::getStatus, OrderInfo::getPayAmount,
                        OrderInfo::getPayTime, OrderInfo::getCreateTime));
        for (OrderInfo order : orders) {
            OrderStat stat = result.computeIfAbsent(order.getUserId(), k -> new OrderStat());
            if (STATUS_CANCELLED.equals(order.getStatus())) {
                continue;
            }
            stat.orderCount++;
            if (order.getPayTime() != null && order.getPayAmount() != null
                    && !OrderStatus.REFUNDED.name().equals(order.getStatus())) {
                stat.paidAmount += order.getPayAmount();
            }
            LocalDateTime createTime = order.getCreateTime();
            if (createTime != null && (stat.lastOrderTime == null || createTime.isAfter(stat.lastOrderTime))) {
                stat.lastOrderTime = createTime;
            }
        }
        return result;
    }

    /** 每个用户有几个收货地址 */
    private Map<Long, Long> addressCounts(Collection<Long> userIds) {
        Map<Long, Long> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<UserAddress> addresses = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .in(UserAddress::getUserId, userIds)
                .select(UserAddress::getUserId));
        for (UserAddress address : addresses) {
            result.merge(address.getUserId(), 1L, Long::sum);
        }
        return result;
    }

    /** 订单行：批量取明细算件数与摘要，避免 N+1 */
    private List<CustomerOrderVO> toCustomerOrders(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(OrderInfo::getId).toList();
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));

        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        for (OrderItem item : items) {
            itemsByOrder.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }

        List<CustomerOrderVO> rows = new ArrayList<>(orders.size());
        for (OrderInfo order : orders) {
            List<OrderItem> lines = itemsByOrder.getOrDefault(order.getId(), List.of());
            int quantity = 0;
            for (OrderItem line : lines) {
                quantity += line.getQuantity() == null ? 0 : line.getQuantity();
            }
            String summary = "—";
            String cover = null;
            if (!lines.isEmpty()) {
                OrderItem first = lines.get(0);
                cover = first.getImageUrl();
                summary = lines.size() == 1
                        ? (first.getProductName() + (quantity > 1 ? " × " + quantity : ""))
                        : first.getProductName() + " 等 " + quantity + " 件";
            }
            OrderStatus status = OrderStatus.of(order.getStatus());
            rows.add(new CustomerOrderVO(order.getId(), order.getOrderNo(), order.getStatus(),
                    status == null ? order.getStatus() : status.getLabel(),
                    order.getPayAmount() == null ? 0L : order.getPayAmount(),
                    quantity, summary, cover, order.getCreateTime()));
        }
        return rows;
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在：" + userId);
        }
        return user;
    }

    private PlatformUserVO toVo(SysUser user, OrderStat stat, long addressCount) {
        int status = user.getStatus() == null ? USER_ENABLED : user.getStatus();
        OrderStat safe = stat == null ? new OrderStat() : stat;
        return new PlatformUserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getMobile(), user.getEmail(),
                user.getPlatformAdmin() != null && user.getPlatformAdmin() == 1,
                status, status == USER_DISABLED ? "停用" : "正常",
                user.getPasswordHash() != null && !user.getPasswordHash().isBlank(),
                safe.orderCount, safe.paidAmount, safe.lastOrderTime, addressCount,
                user.getCreateTime());
    }

    /** 聚合结果的可变载体 */
    private static final class OrderStat {
        private long orderCount;
        private long paidAmount;
        private LocalDateTime lastOrderTime;
    }
}
