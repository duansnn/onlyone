package com.owner.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.convert.SysUserConvert;
import com.owner.system.domain.dto.SysUserCreateDTO;
import com.owner.system.domain.dto.SysUserQuery;
import com.owner.system.domain.dto.SysUserUpdateDTO;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.domain.vo.SysUserVO;
import com.owner.system.mapper.SysUserMapper;
import com.owner.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 门户用户服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private static final int STATUS_ENABLED = 0;

    private final SysUserConvert sysUserConvert;

    @Override
    public PageResult<SysUserVO> pageUsers(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.isNotBlank(query.getNickname()), SysUser::getNickname, query.getNickname())
                .eq(StringUtils.isNotBlank(query.getMobile()), SysUser::getMobile, query.getMobile())
                .eq(StringUtils.isNotBlank(query.getDeptName()), SysUser::getDeptName, query.getDeptName())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = page(new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()), wrapper);
        List<SysUserVO> rows = sysUserConvert.toVoList(page.getRecords());
        rows.forEach(this::maskMobile);
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Cacheable(cacheNames = "sysUser", key = "#id", unless = "#result == null")
    public SysUserVO getUserById(Long id) {
        SysUser entity = getById(id);
        if (entity == null) {
            return null;
        }
        SysUserVO vo = sysUserConvert.toVo(entity);
        maskMobile(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUserCreateDTO dto) {
        long exists = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "登录账号已存在：" + dto.getUsername());
        }
        SysUser entity = sysUserConvert.toEntity(dto);
        entity.setStatus(STATUS_ENABLED);
        entity.setVersion(0);
        save(entity);
        log.info("[SysUser] 新增用户成功 id={}, username={}", entity.getId(), entity.getUsername());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "sysUser", key = "#dto.id")
    public void updateUser(SysUserUpdateDTO dto) {
        SysUser entity = getById(dto.getId());
        if (entity == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在：" + dto.getId());
        }
        sysUserConvert.copyToEntity(dto, entity);
        if (!updateById(entity)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[SysUser] 修改用户成功 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "sysUser", key = "#id")
    public void removeUser(Long id) {
        if (!removeById(id)) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在或已删除：" + id);
        }
        log.info("[SysUser] 删除用户成功 id={}", id);
    }

    private void maskMobile(SysUserVO vo) {
        if (vo != null) {
            vo.setMobile(StringUtils.maskMobile(vo.getMobile()));
        }
    }
}
