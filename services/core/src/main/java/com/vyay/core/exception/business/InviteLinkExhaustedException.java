package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InviteLinkExhaustedException extends BusinessException {
    public InviteLinkExhaustedException() {
        super("This invite link has reached its maximum uses", "ERR_INVITE_LINK_EXHAUSTED", HttpStatus.CONFLICT);
    }
}