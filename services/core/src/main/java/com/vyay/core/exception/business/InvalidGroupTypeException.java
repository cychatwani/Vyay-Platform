package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidGroupTypeException extends BusinessException {

    public InvalidGroupTypeException() {
        super("INDIVIDUAL groups cannot be created directly.", "ERR_INVALID_GROUP_TYPE", HttpStatus.BAD_REQUEST);
    }
}