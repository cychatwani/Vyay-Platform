package com.vyay.core.common.utils;

public class EmailUtils {
    private EmailUtils() {}

    public static String normalize(String email) {
        return email == null
                ? null
                : email.trim().toLowerCase();
    }
}
