package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class UserNotInvitedException extends BusinessException {
    public UserNotInvitedException() {
        super("You Cannot Use this invite to join this group",
                "ERR_USER_NOT_INVITED", HttpStatus.FORBIDDEN);
    }
}