package com.owner.common.core.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 字符串/集合判空与常用处理工具。
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.isEmpty();
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        if (cs == null) {
            return true;
        }
        int len = cs.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /** 空串转 null，非空返回 trim 后的值 */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 空值返回默认值 */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /** 用分隔符连接非空元素 */
    public static String join(CharSequence delimiter, Iterable<?> elements) {
        if (elements == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(delimiter);
        for (Object element : elements) {
            if (element != null) {
                joiner.add(element.toString());
            }
        }
        return joiner.toString();
    }

    /** 下划线转驼峰，如 user_name -> userName */
    public static String toCamelCase(String name) {
        if (isBlank(name) || name.indexOf('_') < 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name.length());
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    /** 驼峰转下划线，如 userName -> user_name */
    public static String toUnderlineCase(String name) {
        if (isBlank(name)) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 脱敏输出常用：手机号 138****8888 */
    public static String maskMobile(String mobile) {
        if (isBlank(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    /** 对象转字符串，null 安全 */
    public static String valueOf(Object obj) {
        return Objects.toString(obj, "");
    }

    /** 比较两个字符串是否相等（null 安全） */
    public static boolean equals(CharSequence a, CharSequence b) {
        return Objects.equals(a, b);
    }
}
