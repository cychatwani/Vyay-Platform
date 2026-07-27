package com.vyay.core.services.auth;

import com.vyay.core.dto.requests.auth.BasePasswordRegisterRequest;
import com.vyay.core.entity.User;

public interface PasswordUserRegistrationHandler<T extends BasePasswordRegisterRequest> {

    Class<T> supports();

    User createUser(T request, String passwordHash);
}