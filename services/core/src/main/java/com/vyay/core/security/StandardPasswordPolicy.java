package com.vyay.core.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StandardPasswordPolicy implements PasswordPolicy {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 72;

    @Override
    public List<String> validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null) {
            violations.add("Password must not be null");
            return violations;
        }

        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters");
        }

        if (password.length() > MAX_LENGTH) {
            violations.add("Password must not exceed " + MAX_LENGTH + " characters");
        }

        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add("Password must contain at least one uppercase letter");
        }

        if (password.chars().noneMatch(Character::isLowerCase)) {
            violations.add("Password must contain at least one lowercase letter");
        }

        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add("Password must contain at least one digit");
        }

        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add("Password must contain at least one special character");
        }

        if (password.contains(" ")) {
            violations.add("Password must not contain spaces");
        }

        return violations;
    }
}