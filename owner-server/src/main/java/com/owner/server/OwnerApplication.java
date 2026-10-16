package com.owner.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Owner IntelliPortal 启动类。
 *
 * @author Owner IntelliPortal Team
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.owner")
public class OwnerApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(OwnerApplication.class, args).getEnvironment();
        printStartupInfo(env);
    }

    private static void printStartupInfo(Environment env) {
        String appName = env.getProperty("spring.application.name", "owner-server");
        String port = env.getProperty("server.port", "41452");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "127.0.0.1";
        }
        String profiles = String.join(",", env.getActiveProfiles());

        log.info("""
                        
                        ----------------------------------------------------------
                          {} 启动成功
                          Profile      : {}
                          Local        : http://localhost:{}{}
                          External     : http://{}:{}{}
                          Knife4j 文档 : http://localhost:{}{}/doc.html
                          Druid 监控   : http://localhost:{}{}/druid/
                        ----------------------------------------------------------""",
                appName, profiles.isEmpty() ? "default" : profiles,
                port, contextPath, host, port, contextPath,
                port, contextPath, port, contextPath);
    }
}
