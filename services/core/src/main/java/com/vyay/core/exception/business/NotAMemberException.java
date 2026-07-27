// NotAMemberException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class NotAMemberException extends BusinessException {
    public NotAMemberException() {
        super("You are not a member of this group.", "ERR_NOT_A_MEMBER", HttpStatus.FORBIDDEN);
    }
}