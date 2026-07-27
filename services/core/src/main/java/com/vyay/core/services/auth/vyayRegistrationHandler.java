package com.vyay.core.services.auth;

import com.vyay.core.common.utils.AvatarUtils;
import com.vyay.core.dto.requests.auth.PasswordRegisterRequestDTO;
import com.vyay.core.entity.User;
import com.vyay.core.services.user.UserService;
import org.springframework.stereotype.Component;

@Component
public class vyayRegistrationHandler implements PasswordUserRegistrationHandler<PasswordRegisterRequestDTO> {

    private final UserService userService;

    public vyayRegistrationHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Class<PasswordRegisterRequestDTO> supports() {
        return PasswordRegisterRequestDTO.class;
    }

    @Override
    public User createUser(PasswordRegisterRequestDTO request, String passwordHash) {
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String avatarUrl = AvatarUtils.generateInitialsAvatarUrl(firstName, lastName);

        return userService.registerPasswordUser(
                firstName,
                lastName,
                request.getEmail(),
                passwordHash,
                avatarUrl
        );
    }
}