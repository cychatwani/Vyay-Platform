package com.vyay.core.common.utils;

import java.util.Map;

public final class TemplateUtils {

    private TemplateUtils() {}

    public static String interpolate(String text, Map<String, String> variables) {
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}