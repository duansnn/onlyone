package com.owner.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置：springdoc 3.0.3 生成 OpenAPI 3，Knife4j 4.5.0 提供增强 UI。
 * <ul>
 *     <li>Knife4j UI：http://localhost:41452/doc.html</li>
 *     <li>Swagger UI：http://localhost:41452/swagger-ui.html</li>
 *     <li>OpenAPI JSON：http://localhost:41452/v3/api-docs</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Value("${spring.application.name:owner-server}")
    private String applicationName;

    @Value("${owner.app.version:2026.03-SNAPSHOT}")
    private String applicationVersion;

    @Bean
    public OpenAPI portalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Owner IntelliPortal 接口文档")
                        .description("企业级智能门户后端 API，应用名：" + applicationName)
                        .version(applicationVersion)
                        .contact(new Contact().name("Owner IntelliPortal Team"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }
}
