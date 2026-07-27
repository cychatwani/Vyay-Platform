package com.vyay.core.services.generateOtp;

import java.util.UUID;

final class OtpRedisSchema {

    private static final String KEY_PREFIX = "otp:EMAIL_VERIFICATION";

    static final String FIELD_USER_ID = "userId";
    static final String FIELD_OTP_HASH = "otpHash";
    static final String FIELD_ATTEMPTS = "attempts";

    private OtpRedisSchema() {
    }

    static String verificationKey(String verificationId) {
        return KEY_PREFIX + ":v:" + verificationId;
    }

    static String userKey(UUID userId) {
        return KEY_PREFIX + ":u:" + userId;
    }

    static String cooldownKey(UUID userId) {
        return KEY_PREFIX + ":cd:" + userId;
    }
}
