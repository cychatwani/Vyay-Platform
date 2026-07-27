package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class AdminCannotLeaveException extends BusinessException {
    public AdminCannotLeaveException() {
        super("A group admin cannot leave; transfer ownership or delete the group first.",
                "ERR_ADMIN_CANNOT_LEAVE", HttpStatus.CONFLICT);
    }
}