package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.CategorySaveDTO;
import com.owner.system.mall.domain.entity.Category;
import com.owner.system.mall.domain.vo.CategoryVO;
import com.owner.system.mall.mapper.CategoryMapper;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 类目服务：店内两级，一级是货架，二级是细分。
 * <p>停用的类目不在店面出现；类目只停用不删除。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    /** 一级类目的父编号 */
    public static final long ROOT_PARENT_ID = 0L;

    private static final int STATUS_ENABLED = 0;
    private static final int STATUS_DISABLED = 1;

    private final CategoryMapper categoryMapper;
    private final ShopGuard shopGuard;

    /** 类目树（后台可含停用） */
    public List<CategoryVO> tree(Long shopId, boolean includeDisabled) {
        shopGuard.requireMember(shopId);
        return buildTree(shopId, includeDisabled);
    }

    /** 类目树（店面只取启用的，公开调用） */
    public List<CategoryVO> activeTree(Long shopId) {
        return buildTree(shopId, false);
    }

    /** 保存类目：id 为空新增，否则修改 */
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long shopId, CategorySaveDTO dto) {
        shopGuard.requireMember(shopId);
        long parentId = dto.parentId() == null ? ROOT_PARENT_ID : dto.parentId();
        validateParent(shopId, parentId, dto.id());

        if (dto.id() == null) {
            Category category = new Category();
            category.setShopId(shopId);
            category.setParentId(parentId);
            category.setName(dto.name().trim());
            category.setSort(dto.sort() == null ? 0 : dto.sort());
            category.setStatus(STATUS_ENABLED);
            category.setVersion(0);
            categoryMapper.insert(category);
            log.info("[Category] 新增类目 shopId={}, id={}, name={}", shopId, category.getId(), category.getName());
            return category.getId();
        }

        Category category = requireCategory(shopId, dto.id());
        category.setParentId(parentId);
        category.setName(dto.name().trim());
        if (dto.sort() != null) {
            category.setSort(dto.sort());
        }
        if (categoryMapper.updateById(category) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Category] 修改类目 shopId={}, id={}", shopId, dto.id());
        return category.getId();
    }

    /** 停用 / 启用类目 */
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long shopId, Long categoryId, int status) {
        shopGuard.requireMember(shopId);
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        Category category = requireCategory(shopId, categoryId);
        if (category.getStatus() != null && category.getStatus() == status) {
            return;
        }
        category.setStatus(status);
        if (categoryMapper.updateById(category) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Category] 类目 {} 状态改为 {}", categoryId, status == STATUS_ENABLED ? "启用" : "停用");
    }

    /** 某个二级类目的名称，取不到返回 null */
    public String nameOf(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    /** 批量取类目名 */
    public java.util.Map<Long, String> namesOf(List<Long> categoryIds) {
        java.util.Map<Long, String> result = new java.util.HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return result;
        }
        for (Category category : categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().in(Category::getId, categoryIds))) {
            result.put(category.getId(), category.getName());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private List<CategoryVO> buildTree(Long shopId, boolean includeDisabled) {
        List<Category> all = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getShopId, shopId)
                .eq(!includeDisabled, Category::getStatus, STATUS_ENABLED)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (Category category : all) {
            if (category.getParentId() != null && category.getParentId() == ROOT_PARENT_ID) {
                List<CategoryVO> children = new ArrayList<>();
                for (Category child : all) {
                    if (category.getId().equals(child.getParentId())) {
                        children.add(toVo(child, List.of()));
                    }
                }
                roots.add(toVo(category, children));
            }
        }
        return roots;
    }

    private CategoryVO toVo(Category category, List<CategoryVO> children) {
        return new CategoryVO(category.getId(), category.getParentId(), category.getName(),
                category.getSort(), category.getStatus(), statusLabel(category.getStatus()), children);
    }

    private Category requireCategory(Long shopId, Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getShopId().equals(shopId)) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "类目不存在：" + categoryId);
        }
        return category;
    }

    /**
     * 校验父类目：只能是 0 或本店的一级类目；
     * 且已经带子类目的一级类目不能再挂到别的类目下（只做两级）。
     */
    private void validateParent(Long shopId, long parentId, Long selfId) {
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        if (selfId != null && selfId == parentId) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "父类目不能是自己");
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null || !parent.getShopId().equals(shopId)) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "父类目不存在：" + parentId);
        }
        if (parent.getParentId() != null && parent.getParentId() != ROOT_PARENT_ID) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "类目只支持两级");
        }
        if (selfId != null) {
            long childCount = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                    .eq(Category::getShopId, shopId)
                    .eq(Category::getParentId, selfId));
            if (childCount > 0) {
                throw new ServiceException(ResultCode.BAD_REQUEST, "该类目下还有子类目，不能再挂到别的类目下");
            }
        }
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == STATUS_DISABLED ? "停用" : "启用";
    }

    /** 供其它服务复用的名称去空格工具 */
    static String trim(String value) {
        return StringUtils.trimToNull(value);
    }
}
