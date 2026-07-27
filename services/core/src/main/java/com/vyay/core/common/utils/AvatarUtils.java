package com.vyay.core.common.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class AvatarUtils {

    private static final String DEFAULT_BASE_URL = "https://ui-avatars.com/api/";

    private static final String[] COLORS = {
            "3B82F6", // Blue
            "2563EB", // Deep Blue
            "7C3AED", // Purple
            "9333EA", // Violet
            "DB2777", // Pink
            "EA580C", // Orange
            "059669", // Emerald
            "0D9488", // Teal
            "0891B2", // Cyan
            "475569", // Slate
            "4338CA", // Indigo
            "BE185D", // Dark Rose
            "166534", // Forest Green
            "0F766E", // Dark Teal
            "374151", // Dark Gray
            "B91C1C"  // Dark Red
    };

    private AvatarUtils() {
    }

    public static String generateInitialsAvatarUrl(String firstName, String lastName) {
        return generateInitialsAvatarUrl(firstName, lastName, DEFAULT_BASE_URL);
    }

    public static String generateInitialsAvatarUrl(String firstName,
                                                   String lastName,
                                                   String baseUrl) {

        String initials =
                String.valueOf(firstName.trim().charAt(0)).toUpperCase()
                        + String.valueOf(lastName.trim().charAt(0)).toUpperCase();

        String color = COLORS[
                ThreadLocalRandom.current().nextInt(COLORS.length)
                ];

        return baseUrl + "?name=" + initials
                + "&background=" + color
                + "&color=ffffff"
                + "&size=128"
                + "&bold=true";
    }
}