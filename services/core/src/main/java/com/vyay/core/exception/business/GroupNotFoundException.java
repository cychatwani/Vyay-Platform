// GroupNotFoundException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class GroupNotFoundException extends BusinessException {
    public GroupNotFoundException() {
        super("Group not found.", "ERR_GROUP_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}