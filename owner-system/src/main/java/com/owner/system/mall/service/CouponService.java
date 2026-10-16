package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.dto.CouponSaveDTO;
import com.owner.system.mall.domain.entity.Coupon;
import com.owner.system.mall.domain.entity.CouponUser;
import com.owner.system.mall.domain.enums.CouponUserStatus;
import com.owner.system.mall.domain.vo.CouponUserVO;
import com.owner.system.mall.domain.vo.CouponVO;
import com.owner.system.mall.mapper.CouponMapper;
import com.owner.system.mall.mapper.CouponUserMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ShopGuard;
import com.owner.system.mapper.SysUserMapper;
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
 * 优惠券服务。
 *
 * <p>规则（PRD §11）：减免金额必须小于门槛；商品总额达到门槛才能用，运费不参与；
 * 下单时占用，支付成功后核销，取消或超时未支付则退回；过期券不能领、不能用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private static final int COUPON_ENABLED = 0;
    private static final int COUPON_DISABLED = 1;

    private final CouponMapper couponMapper;
    private final CouponUserMapper couponUserMapper;
    private final SysUserMapper sysUserMapper;
    private final ShopGuard shopGuard;

    // ------------------------------------------------------------------
    // 店铺后台
    // ------------------------------------------------------------------

    /** 店里的券（店主与店员都能看） */
    public PageResult<CouponVO> shopCoupons(Long shopId, String status, PageQuery query) {
        shopGuard.requireMember(shopId);
        Integer statusFilter = parseStatus(status);
        Page<Coupon> page = couponMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getShopId, shopId)
                        .eq(statusFilter != null, Coupon::getStatus, statusFilter)
                        .orderByDesc(Coupon::getCreateTime));
        List<CouponVO> rows = page.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 建券 / 改券（仅店主） */
    @Transactional(rollbackFor = Exception.class)
    public Long saveCoupon(Long shopId, CouponSaveDTO dto) {
        shopGuard.requireOwner(shopId);
        if (dto.reduceAmount() >= dto.thresholdAmount()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "减免金额必须小于门槛金额");
        }
        if (dto.startTime() != null && dto.endTime() != null && !dto.endTime().isAfter(dto.startTime())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        int status = "1".equals(dto.status()) ? COUPON_DISABLED : COUPON_ENABLED;

        if (dto.id() == null) {
            Coupon coupon = new Coupon();
            coupon.setShopId(shopId);
            coupon.setName(dto.name().trim());
            coupon.setThresholdAmount(dto.thresholdAmount());
            coupon.setReduceAmount(dto.reduceAmount());
            coupon.setTotal(dto.total());
            coupon.setReceived(0);
            coupon.setPerUserLimit(dto.perUserLimit() == null ? 1 : dto.perUserLimit());
            coupon.setStartTime(dto.startTime());
            coupon.setEndTime(dto.endTime());
            coupon.setStatus(status);
            coupon.setVersion(0);
            couponMapper.insert(coupon);
            log.info("[Coupon] 新建优惠券 shopId={}, couponId={}, name={}", shopId, coupon.getId(), coupon.getName());
            return coupon.getId();
        }

        Coupon coupon = requireCoupon(shopId, dto.id());
        if (dto.total() < (coupon.getReceived() == null ? 0 : coupon.getReceived())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "发放数量不能小于已领取数量");
        }
        coupon.setName(dto.name().trim());
        coupon.setThresholdAmount(dto.thresholdAmount());
        coupon.setReduceAmount(dto.reduceAmount());
        coupon.setTotal(dto.total());
        coupon.setPerUserLimit(dto.perUserLimit() == null ? coupon.getPerUserLimit() : dto.perUserLimit());
        coupon.setStartTime(dto.startTime());
        coupon.setEndTime(dto.endTime());
        coupon.setStatus(status);
        if (couponMapper.updateById(coupon) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Coupon] 修改优惠券 shopId={}, couponId={}", shopId, coupon.getId());
        return coupon.getId();
    }

    /** 停用 / 启用券（仅店主） */
    @Transactional(rollbackFor = Exception.class)
    public void setCouponStatus(Long shopId, Long couponId, int status) {
        shopGuard.requireOwner(shopId);
        if (status != COUPON_ENABLED && status != COUPON_DISABLED) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        Coupon coupon = requireCoupon(shopId, couponId);
        coupon.setStatus(status);
        if (couponMapper.updateById(coupon) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
    }

    /** 店主把券发给指定用户（按手机号） */
    @Transactional(rollbackFor = Exception.class)
    public void grantToUser(Long shopId, Long couponId, String mobile) {
        shopGuard.requireOwner(shopId);
        requireCoupon(shopId, couponId);
        if (StringUtils.isBlank(mobile)) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getMobile, mobile.trim())
                .last("LIMIT 1"));
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "该手机号还没有用户");
        }
        doReceive(couponId, user.getId());
        log.info("[Coupon] 店主发券 shopId={}, couponId={}, userId={}", shopId, couponId, user.getId());
    }

    // ------------------------------------------------------------------
    // 顾客侧
    // ------------------------------------------------------------------

    /** 领券中心：可领的券 */
    public PageResult<CouponVO> receiveCenter(Long shopId, PageQuery query) {
        shopGuard.loadActiveShop(shopId);
        Page<Coupon> page = couponMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getShopId, shopId)
                        .eq(Coupon::getStatus, COUPON_ENABLED)
                        .apply("received < total")
                        .orderByDesc(Coupon::getCreateTime));
        List<CouponVO> rows = page.getRecords().stream()
                .filter(coupon -> !isExpired(coupon))
                .map(this::toVo)
                .toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 领券 */
    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long couponId) {
        Long userId = MallCurrentUser.requireUserId();
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "优惠券不存在");
        }
        shopGuard.loadActiveShop(coupon.getShopId());
        return doReceive(couponId, userId);
    }

    /** 我的券 */
    @Transactional(rollbackFor = Exception.class)
    public List<CouponUserVO> myCoupons(Long shopId, String status) {
        Long userId = MallCurrentUser.requireUserId();
        List<CouponUser> records = couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(shopId != null, CouponUser::getShopId, shopId)
                .orderByDesc(CouponUser::getReceiveTime));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Coupon> couponMap = couponsOf(records.stream().map(CouponUser::getCouponId).distinct().toList());

        List<CouponUserVO> rows = new ArrayList<>(records.size());
        for (CouponUser record : records) {
            Coupon coupon = couponMap.get(record.getCouponId());
            // 懒过期：已到期的未使用券标记为已过期
            if (coupon != null && isExpired(coupon) && isAvailable(record.getStatus())) {
                record.setStatus(CouponUserStatus.EXPIRED.name());
                couponUserMapper.updateById(record);
            }
            CouponUserStatus userStatus = CouponUserStatus.of(record.getStatus());
            if (StringUtils.isNotBlank(status) && (userStatus == null
                    || !status.trim().toUpperCase().equals(userStatus.name()))) {
                continue;
            }
            rows.add(toUserVo(record, coupon));
        }
        return rows;
    }

    /** 结算时可用的券：门槛已满足、在有效期内、状态可再用，且属于本店 */
    public List<CouponUserVO> usableCoupons(Long userId, Long shopId, long goodsAmount) {
        List<CouponUser> records = couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getShopId, shopId)
                .in(CouponUser::getStatus, CouponUserStatus.UNUSED.name(), CouponUserStatus.RETURNED.name()));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Coupon> couponMap = couponsOf(records.stream().map(CouponUser::getCouponId).distinct().toList());
        List<CouponUserVO> rows = new ArrayList<>();
        for (CouponUser record : records) {
            Coupon coupon = couponMap.get(record.getCouponId());
            if (coupon == null || !isUsableNow(coupon)) {
                continue;
            }
            long threshold = coupon.getThresholdAmount() == null ? 0 : coupon.getThresholdAmount();
            if (goodsAmount < threshold) {
                continue;
            }
            rows.add(toUserVo(record, coupon));
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 订单流程
    // ------------------------------------------------------------------

    /** 占用优惠券：校验后置为 LOCKED，返回实际减免额（分） */
    public long lock(Long userId, Long shopId, Long couponUserId, long goodsAmount, Long orderId) {
        CouponUser record = requireOwnCouponUser(userId, couponUserId);
        if (!isAvailable(record.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该优惠券当前不可用");
        }
        Coupon coupon = requireCoupon(shopId, record.getCouponId());
        if (!isUsableNow(coupon)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券已过期或已停用");
        }
        long threshold = coupon.getThresholdAmount() == null ? 0 : coupon.getThresholdAmount();
        if (goodsAmount < threshold) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "商品总额未达到优惠券门槛，需满 " + threshold + " 分");
        }
        record.setStatus(CouponUserStatus.LOCKED.name());
        record.setOrderId(orderId);
        couponUserMapper.updateById(record);
        return coupon.getReduceAmount() == null ? 0 : coupon.getReduceAmount();
    }

    /** 支付成功：把该订单占用的券核销 */
    public void useByOrder(Long orderId) {
        List<CouponUser> records = lockedByOrder(orderId);
        for (CouponUser record : records) {
            record.setStatus(CouponUserStatus.USED.name());
            record.setUseTime(LocalDateTime.now());
            couponUserMapper.updateById(record);
        }
    }

    /** 取消 / 超时未支付：把券退回 */
    public void returnByOrder(Long orderId) {
        List<CouponUser> records = lockedByOrder(orderId);
        for (CouponUser record : records) {
            record.setStatus(CouponUserStatus.RETURNED.name());
            record.setOrderId(null);
            couponUserMapper.updateById(record);
            log.info("[Coupon] 订单 {} 取消，券 {} 已退回", orderId, record.getCouponId());
        }
    }

    /** 门店对某订单是否占用了券 */
    private List<CouponUser> lockedByOrder(Long orderId) {
        return couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getOrderId, orderId)
                .eq(CouponUser::getStatus, CouponUserStatus.LOCKED.name()));
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 领取的公共实现 */
    private Long doReceive(Long couponId, Long userId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != COUPON_ENABLED) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券已停用或不存在");
        }
        if (isExpired(coupon)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券已过期");
        }
        if (coupon.getStartTime() != null && LocalDateTime.now().isBefore(coupon.getStartTime())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券还没到可领取时间");
        }
        int perUserLimit = coupon.getPerUserLimit() == null ? 1 : coupon.getPerUserLimit();
        long mine = couponUserMapper.selectCount(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getCouponId, couponId)
                .eq(CouponUser::getUserId, userId));
        if (mine >= perUserLimit) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "每人限领 " + perUserLimit + " 张，你已领完");
        }
        if (couponMapper.increaseReceived(couponId) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "优惠券已领完");
        }

        CouponUser record = new CouponUser();
        record.setCouponId(couponId);
        record.setShopId(coupon.getShopId());
        record.setUserId(userId);
        record.setStatus(CouponUserStatus.UNUSED.name());
        record.setReceiveTime(LocalDateTime.now());
        record.setVersion(0);
        couponUserMapper.insert(record);
        log.info("[Coupon] 领取成功 couponId={}, userId={}, couponUserId={}", couponId, userId, record.getId());
        return record.getId();
    }

    private Coupon requireCoupon(Long shopId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !shopId.equals(coupon.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "优惠券不存在：" + couponId);
        }
        return coupon;
    }

    private CouponUser requireOwnCouponUser(Long userId, Long couponUserId) {
        CouponUser record = couponUserMapper.selectById(couponUserId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "优惠券不存在");
        }
        return record;
    }

    private Map<Long, Coupon> couponsOf(List<Long> couponIds) {
        if (couponIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Coupon> result = new HashMap<>(couponIds.size() * 2);
        for (Coupon coupon : couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .in(Coupon::getId, couponIds))) {
            result.put(coupon.getId(), coupon);
        }
        return result;
    }

    private boolean isDeletedOrDisabled(Coupon coupon) {
        return coupon.getStatus() == null || coupon.getStatus() != COUPON_ENABLED;
    }

    /** 现在是否可用：券启用、已在有效期内、未过期 */
    private boolean isUsableNow(Coupon coupon) {
        if (coupon == null || isDeletedOrDisabled(coupon) || isExpired(coupon)) {
            return false;
        }
        return coupon.getStartTime() == null || !LocalDateTime.now().isBefore(coupon.getStartTime());
    }

    private boolean isExpired(Coupon coupon) {
        return coupon.getEndTime() != null && LocalDateTime.now().isAfter(coupon.getEndTime());
    }

    private boolean isAvailable(String status) {
        CouponUserStatus userStatus = CouponUserStatus.of(status);
        return userStatus != null && userStatus.isAvailable();
    }

    private Integer parseStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        try {
            return Integer.valueOf(status.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CouponVO toVo(Coupon coupon) {
        int received = coupon.getReceived() == null ? 0 : coupon.getReceived();
        int total = coupon.getTotal() == null ? 0 : coupon.getTotal();
        return new CouponVO(coupon.getId(), coupon.getShopId(), coupon.getName(),
                coupon.getThresholdAmount(), coupon.getReduceAmount(), coupon.getTotal(),
                coupon.getReceived(), coupon.getPerUserLimit(), coupon.getStartTime(),
                coupon.getEndTime(), coupon.getStatus(), couponStatusLabel(coupon.getStatus()),
                isExpired(coupon), !isDeletedOrDisabled(coupon) && received < total && !isExpired(coupon),
                coupon.getCreateTime());
    }

    private CouponUserVO toUserVo(CouponUser record, Coupon coupon) {
        CouponUserStatus status = CouponUserStatus.of(record.getStatus());
        return new CouponUserVO(record.getId(), record.getCouponId(), record.getShopId(),
                coupon == null ? null : coupon.getName(),
                coupon == null ? null : coupon.getThresholdAmount(),
                coupon == null ? null : coupon.getReduceAmount(),
                coupon == null ? null : coupon.getPerUserLimit(),
                coupon == null ? null : coupon.getStartTime(),
                coupon == null ? null : coupon.getEndTime(),
                record.getStatus(), status == null ? record.getStatus() : status.getLabel(),
                record.getOrderId(), coupon != null && isExpired(coupon),
                record.getReceiveTime(), record.getUseTime());
    }

    private String couponStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == COUPON_DISABLED ? "停用" : "启用";
    }
}
