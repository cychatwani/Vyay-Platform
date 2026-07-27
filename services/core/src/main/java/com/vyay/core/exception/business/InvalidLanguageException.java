package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidLanguageException extends BusinessException {

    public InvalidLanguageException(String code) {
        super("Language not supported: " + code, "ERR_INVALID_LANGUAGE", HttpStatus.BAD_REQUEST);
    }
}