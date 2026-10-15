package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.kanban.domain.dto.LoginDTO;
import com.owner.system.kanban.domain.dto.RegisterDTO;
import com.owner.system.kanban.domain.entity.KanbanUser;
import com.owner.system.kanban.domain.vo.LoginVO;
import com.owner.system.kanban.mapper.KanbanUserMapper;
import com.owner.system.kanban.support.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 看板账号服务：注册（BCrypt）/ 登录（JWT）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanAuthService {

    private final KanbanUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /** 注册：用户名唯一，密码 BCrypt 加密落库。 */
    public Long register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                Wrappers.<KanbanUser>lambdaQuery().eq(KanbanUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "用户名已被注册：" + dto.getUsername());
        }
        KanbanUser user = new KanbanUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userMapper.insert(user);
        log.info("[Kanban] 用户注册成功：{} (id={})", dto.getUsername(), user.getId());
        return user.getId();
    }

    /** 登录：校验通过后签发 JWT。 */
    public LoginVO login(LoginDTO dto) {
        KanbanUser user = userMapper.selectOne(
                Wrappers.<KanbanUser>lambdaQuery().eq(KanbanUser::getUsername, dto.getUsername()));
        // 用户不存在与密码错误统一提示，避免用户名枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "用户名或密码错误");
        }
        String token = jwtTokenService.createToken(user.getId(), user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), jwtTokenService.expireAtMillis());
    }
}
