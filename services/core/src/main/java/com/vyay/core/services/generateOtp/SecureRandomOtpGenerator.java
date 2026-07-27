package com.vyay.core.services.generateOtp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomOtpGenerator implements OtpGenerator {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 8;

    private static final int[] POW10 = {
            1,
            10,
            100,
            1_000,
            10_000,
            100_000,
            1_000_000,
            10_000_000,
            100_000_000
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate(int length) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "OTP length must be between " + MIN_LENGTH + " and " + MAX_LENGTH +
                            ", got: " + length
            );
        }

        int origin = POW10[length - 1];
        int bound = POW10[length];

        return String.valueOf(RANDOM.nextInt(origin, bound));
    }
}