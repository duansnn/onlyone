package com.owner;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.JsonUtils;
import com.owner.common.core.utils.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单元测试（不依赖 MySQL / Redis，可在构建阶段直接执行）。
 */
class OwnerApplicationTests {

    @Test
    @DisplayName("统一返回结构正常")
    void testResultWrapper() {
        R<String> ok = R.ok("hello");
        assertEquals(200, ok.getCode());
        assertTrue(ok.isSuccess());
        assertEquals("hello", ok.getData());

        R<Void> fail = R.fail("出错了");
        assertEquals(500, fail.getCode());
        assertFalse(fail.isSuccess());
    }

    @Test
    @DisplayName("分页结果能正确计算总页数")
    void testPageResult() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 21, 2, 10);
        assertEquals(3, page.getPages());
        assertEquals(21, page.getTotal());

        PageQuery query = new PageQuery();
        query.setPageNum(0L);
        query.setPageSize(99999L);
        assertEquals(1L, query.getPageNumOrDefault());
        assertEquals(PageQuery.MAX_PAGE_SIZE, query.getPageSizeOrDefault());
    }

    @Test
    @DisplayName("字符串与 JSON 工具正常")
    void testUtils() {
        assertTrue(StringUtils.isBlank("  "));
        assertEquals("userName", StringUtils.toCamelCase("user_name"));
        assertEquals("user_name", StringUtils.toUnderlineCase("userName"));
        assertEquals("138****0000", StringUtils.maskMobile("13800000000"));

        String json = JsonUtils.toJson(java.util.Map.of("type", "notice"));
        assertNotNull(json);
        assertTrue(json.contains("notice"));
    }

    @Test
    @DisplayName("业务异常携带状态码")
    void testServiceException() {
        ServiceException ex = new ServiceException("参数不合法");
        assertEquals(600, ex.getCode());
        assertEquals("参数不合法", ex.getMessage());
        assertThrows(ServiceException.class, () -> {
            throw ServiceException.of("测试异常");
        });
    }
}
