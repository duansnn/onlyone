package com.owner.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.owner.common.core.domain.PageResult;
import com.owner.system.domain.dto.SysUserCreateDTO;
import com.owner.system.domain.dto.SysUserQuery;
import com.owner.system.domain.dto.SysUserUpdateDTO;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.domain.vo.SysUserVO;

/**
 * 门户用户服务。
 */
public interface ISysUserService extends IService<SysUser> {

    /** 分页查询 */
    PageResult<SysUserVO> pageUsers(SysUserQuery query);

    /** 按主键查询（走 Redis 缓存） */
    SysUserVO getUserById(Long id);

    /** 新增 */
    Long createUser(SysUserCreateDTO dto);

    /** 修改（乐观锁） */
    void updateUser(SysUserUpdateDTO dto);

    /** 删除（逻辑删除） */
    void removeUser(Long id);

    /** 发送欢迎 WebSocket 消息 */
    void pushWelcome(Long userId);
}
