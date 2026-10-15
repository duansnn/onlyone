package com.owner.framework.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring 上下文工具，用于在非托管对象中获取 Bean。
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        SpringUtils.applicationContext = context;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext == null ? null : applicationContext.getBean(clazz);
    }

    public static Object getBean(String name) {
        return applicationContext == null ? null : applicationContext.getBean(name);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext == null ? null : applicationContext.getBean(name, clazz);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return applicationContext == null ? Map.of() : applicationContext.getBeansOfType(clazz);
    }

    public static String getProperty(String key) {
        return applicationContext == null ? null : applicationContext.getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return applicationContext == null ? defaultValue
                : applicationContext.getEnvironment().getProperty(key, defaultValue);
    }

    public static boolean isActiveProfile(String profile) {
        if (applicationContext == null) {
            return false;
        }
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        for (String active : activeProfiles) {
            if (active.equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
