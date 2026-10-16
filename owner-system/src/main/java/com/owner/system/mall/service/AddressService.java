package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.AddressSaveDTO;
import com.owner.system.mall.domain.entity.UserAddress;
import com.owner.system.mall.domain.vo.AddressVO;
import com.owner.system.mall.mapper.UserAddressMapper;
import com.owner.system.mall.support.MallCurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 收货地址服务。地址属于用户本人，不属于某一家店；每人最多 20 条；
 * 默认地址只有一条，新的默认会取消原来的默认。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    /** 每人最多地址条数 */
    private static final int MAX_ADDRESS = 20;

    private static final int IS_DEFAULT = 1;
    private static final int NOT_DEFAULT = 0;

    private final UserAddressMapper addressMapper;

    /** 我的地址列表，默认地址排最前 */
    public List<AddressVO> list() {
        Long userId = MallCurrentUser.requireUserId();
        List<UserAddress> addresses = addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
        List<AddressVO> rows = new ArrayList<>(addresses.size());
        for (UserAddress address : addresses) {
            rows.add(toVo(address));
        }
        return rows;
    }

    /** 取自己的某条地址 */
    public UserAddress requireOwnAddress(Long userId, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    /** 新增 / 修改地址 */
    @Transactional(rollbackFor = Exception.class)
    public Long save(AddressSaveDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        boolean wantDefault = Boolean.TRUE.equals(dto.isDefault());

        if (dto.id() == null) {
            long count = addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                    .eq(UserAddress::getUserId, userId));
            if (count >= MAX_ADDRESS) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR, "每人最多保存 " + MAX_ADDRESS + " 条地址");
            }
            UserAddress address = new UserAddress();
            address.setUserId(userId);
            applyFields(address, dto);
            address.setIsDefault(NOT_DEFAULT);
            address.setVersion(0);
            addressMapper.insert(address);

            // 第一条地址自动成为默认
            if (wantDefault || count == 0) {
                setDefault(userId, address.getId());
            }
            log.info("[Address] 新增地址 userId={}, addressId={}", userId, address.getId());
            return address.getId();
        }

        UserAddress address = requireOwnAddress(userId, dto.id());
        applyFields(address, dto);
        if (addressMapper.updateById(address) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        if (wantDefault) {
            setDefault(userId, address.getId());
        }
        log.info("[Address] 修改地址 userId={}, addressId={}", userId, address.getId());
        return address.getId();
    }

    /** 设为默认：先把其余置为非默认 */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId) {
        Long userId = MallCurrentUser.requireUserId();
        requireOwnAddress(userId, addressId);
        setDefault(userId, addressId);
    }

    /** 删除地址 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long addressId) {
        Long userId = MallCurrentUser.requireUserId();
        UserAddress address = requireOwnAddress(userId, addressId);
        boolean wasDefault = address.getIsDefault() != null && address.getIsDefault() == IS_DEFAULT;
        addressMapper.deleteById(addressId);
        if (wasDefault) {
            // 默认地址被删掉后，把最近更新的一条设为默认，避免下单时没有默认地址
            UserAddress next = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdateTime)
                    .last("LIMIT 1"));
            if (next != null) {
                setDefault(userId, next.getId());
            }
        }
        log.info("[Address] 删除地址 userId={}, addressId={}", userId, addressId);
    }

    /** 供订单模块使用：把地址转成快照字符串 */
    public static String fullAddress(UserAddress address) {
        return AddressVO.join(address.getProvince(), address.getCity(),
                address.getDistrict(), address.getDetail());
    }

    private void setDefault(Long userId, Long addressId) {
        UserAddress clear = new UserAddress();
        clear.setIsDefault(NOT_DEFAULT);
        addressMapper.update(clear, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, IS_DEFAULT));

        UserAddress target = new UserAddress();
        target.setId(addressId);
        target.setIsDefault(IS_DEFAULT);
        addressMapper.updateById(target);
    }

    private void applyFields(UserAddress address, AddressSaveDTO dto) {
        address.setReceiver(dto.receiver().trim());
        address.setMobile(dto.mobile().trim());
        address.setProvince(StringUtils.trimToNull(dto.province()));
        address.setCity(StringUtils.trimToNull(dto.city()));
        address.setDistrict(StringUtils.trimToNull(dto.district()));
        address.setDetail(dto.detail().trim());
    }

    private AddressVO toVo(UserAddress address) {
        return new AddressVO(address.getId(), address.getReceiver(), address.getMobile(),
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetail(),
                fullAddress(address), address.getIsDefault());
    }
}
