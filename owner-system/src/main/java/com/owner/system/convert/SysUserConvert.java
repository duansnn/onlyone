package com.owner.system.convert;

import com.owner.system.domain.dto.SysUserCreateDTO;
import com.owner.system.domain.dto.SysUserUpdateDTO;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.domain.vo.SysUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 门户用户对象转换（MapStruct 1.6.3）。
 * <p>componentModel 已在编译期统一设置为 spring。</p>
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysUserConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SysUser toEntity(SysUserCreateDTO dto);

    /** 手机号在 Service 层统一脱敏后覆盖 */
    SysUserVO toVo(SysUser entity);

    List<SysUserVO> toVoList(List<SysUser> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void copyToEntity(SysUserUpdateDTO dto, @MappingTarget SysUser entity);
}
