package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InviteLinkExpiredException extends BusinessException {
    public InviteLinkExpiredException() {
        super("This invite link has expired", "ERR_INVITE_LINK_EXPIRED", HttpStatus.GONE);
    }
}