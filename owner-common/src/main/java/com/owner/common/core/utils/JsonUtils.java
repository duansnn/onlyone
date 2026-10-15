package com.owner.common.core.utils;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 Jackson 3 的 JSON 工具（Spring Boot 4 默认 JSON 实现，java.time 已内置支持）。
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** 对象转 JSON 字符串，失败返回 null */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("[JsonUtils] 序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /** JSON 字符串转对象，失败返回 null */
    public static <T> T parse(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.error("[JsonUtils] 反序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /** JSON 字符串转复杂泛型对象，失败返回 null */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json) || typeReference == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("[JsonUtils] 反序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /** JSON 字符串转 List，失败返回空集合 */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.error("[JsonUtils] 反序列化 List 失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** JSON 字符串转 Map，失败返回空 Map */
    public static Map<String, Object> parseMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("[JsonUtils] 反序列化 Map 失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /** 读取 JSON 树 */
    public static JsonNode readTree(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            log.error("[JsonUtils] 读取 JSON 树失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /** 对象类型转换 */
    public static <T> T convert(Object source, Class<T> clazz) {
        if (source == null || clazz == null) {
            return null;
        }
        return MAPPER.convertValue(source, clazz);
    }
}
