// AdminOnlyException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class AdminOnlyException extends BusinessException {
    public AdminOnlyException() {
        super("Only a group admin can perform this action.", "ERR_ADMIN_ONLY", HttpStatus.FORBIDDEN);
    }
}