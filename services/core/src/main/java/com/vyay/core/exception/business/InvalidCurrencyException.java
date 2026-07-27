package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidCurrencyException extends BusinessException {

    public InvalidCurrencyException(String code) {
        super("Currency not supported: " + code, "ERR_INVALID_CURRENCY", HttpStatus.BAD_REQUEST);
    }
}