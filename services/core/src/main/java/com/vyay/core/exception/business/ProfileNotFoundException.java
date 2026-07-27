package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends BusinessException {

    public ProfileNotFoundException() {
        super("Profile not found", "ERR_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}