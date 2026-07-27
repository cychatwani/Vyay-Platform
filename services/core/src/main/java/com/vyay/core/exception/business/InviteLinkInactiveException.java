package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InviteLinkInactiveException extends BusinessException {
    public InviteLinkInactiveException() {
        super("This invite link is no longer active", "ERR_INVITE_LINK_INACTIVE", HttpStatus.CONFLICT);
    }
}