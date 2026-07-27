package com.vyay.core.services.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class NotificationCommand {

    private final String type;
    private final String channel;
    private final String recipient;
    private final Map<String, String> variables;
}