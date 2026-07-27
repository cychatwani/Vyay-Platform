package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InviteLinkNotFoundException extends BusinessException {
    public InviteLinkNotFoundException() {
        super("Invite link not found", "ERR_INVITE_LINK_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}