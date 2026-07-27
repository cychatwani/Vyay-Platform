// AlreadyAMemberException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class AlreadyAMemberException extends BusinessException {
    public AlreadyAMemberException(String message) {
        super(message, "ERR_ALREADY_A_MEMBER", HttpStatus.CONFLICT);
    }
}