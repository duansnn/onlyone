package com.owner.system.controller;

import com.owner.common.core.domain.R;
import com.owner.framework.quartz.PortalHeartbeatJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户运行状态。
 */
@Slf4j
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Tag(name = "门户运行状态", description = "应用信息与仪表盘总览")
public class PortalController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Environment environment;
    private final com.owner.system.service.PortalOverviewService portalOverviewService;

    @Value("${spring.application.name:owner-server}")
    private String applicationName;

    @Value("${server.port:41452}")
    private int serverPort;

    @Operation(summary = "应用信息与运行状态")
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applicationName", applicationName);
        data.put("port", serverPort);
        data.put("activeProfiles", String.join(",", environment.getActiveProfiles()));
        data.put("serverTime", FORMATTER.format(LocalDateTime.now()));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("quartzHeartbeatExecutions", PortalHeartbeatJob.executionCount());
        return R.ok(data);
    }

    @Operation(summary = "仪表盘总览")
    @GetMapping("/overview")
    public R<com.owner.system.domain.vo.PortalOverviewVO> overview(
            @Parameter(description = "趋势天数，默认 14") @RequestParam(defaultValue = "14") int trendDays) {
        return R.ok(portalOverviewService.overview(Math.min(Math.max(trendDays, 1), 90)));
    }
}
