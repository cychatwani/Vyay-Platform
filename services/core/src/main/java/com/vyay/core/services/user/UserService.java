package com.vyay.core.services.user;


import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.exception.auth.EmailAlreadyRegisteredException;
import com.vyay.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getById(UUID userId) {
        return userRepository.findById(userId);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }

    public User registerPasswordUser(String firstName, String lastName, String email,
                                     String passwordHash, String profilePicture) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordHash)
                .profilePicture(profilePicture)
                .authProvider(AuthProvider.PASSWORD)
                .emailVerified(false)
                .build();

        return userRepository.save(user);
    }


    public User createIfNotExists(String firstName, String lastName, String email, String profilePicture, AuthProvider provider, boolean emailVerified) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .profilePicture(profilePicture)
                .authProvider(provider)
                .emailVerified(emailVerified)
                .build();
        return userRepository.save(newUser);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

}
