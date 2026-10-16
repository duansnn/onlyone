package com.owner.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper（MyBatis-Plus 3.5.16 + MyBatis 3.5.19）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
