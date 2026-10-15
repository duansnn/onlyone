package com.owner.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 3 序列化定制。
 *
 * <p><b>为什么要把 Long 序列化成字符串？</b></p>
 * <p>主键使用雪花算法（MyBatis-Plus {@code IdType.ASSIGN_ID}），生成的 ID 形如
 * {@code 2110285652501422082}，已超过 JavaScript 的
 * {@code Number.MAX_SAFE_INTEGER}（9007199254740991）。
 * 浏览器用 {@code JSON.parse} 解析时会丢失精度，ID 被四舍五入成
 * {@code 2110285652501422000}，导致前端回传后后端报「用户不存在」。</p>
 *
 * <p>因此把包装类型 {@link Long} 统一序列化为 JSON 字符串（前端以字符串收发 ID）；
 * 而原始类型 {@code long}（如 {@code PageResult.total}、{@code R.timestamp}）仍保持数字，
 * 避免影响分页与时间戳等常规数值字段。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer ownerLongAsStringCustomizer() {
        log.info("[Jackson] 已注册 Long -> String 序列化器（雪花 ID 精度保护）");
        return builder -> builder.addModule(
                new SimpleModule("ownerLongAsStringModule")
                        .addSerializer(Long.class, ToStringSerializer.instance));
    }
}
