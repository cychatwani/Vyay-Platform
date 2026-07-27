package com.vyay.core.services.notification;

import java.util.Collection;
import java.util.Map;

public interface NotificationService {

    void send(String type, String channel, String recipient, Map<String, String> variables);

    void send(Collection<NotificationCommand> commands);
}