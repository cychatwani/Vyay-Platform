package com.vyay.core.entity;

import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String type; // e.g. "EMAIL_VERIFICATION", "PASSWORD_RESET"

    @Column(nullable = false)
    private String subject; // e.g. "Verify your email"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body; // e.g. "Hi {{firstName}}, click here: {{link}}"

    @Column(nullable = false, length = 20)
    private String channel; // e.g. "EMAIL", "SMS", "PUSH"
}