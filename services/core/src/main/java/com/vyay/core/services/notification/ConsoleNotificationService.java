package com.vyay.core.services.notification;

import com.vyay.core.common.utils.TemplateUtils;
import com.vyay.core.entity.NotificationTemplate;
import com.vyay.core.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);

    private final NotificationTemplateRepository templateRepository;

    public ConsoleNotificationService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public void send(String type, String channel, String recipient, Map<String, String> variables) {
        NotificationTemplate template = templateRepository.findByTypeAndChannel(type, channel)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No template found for type=" + type + ", channel=" + channel));

        String subject = TemplateUtils.interpolate(template.getSubject(), variables);
        String body = TemplateUtils.interpolate(template.getBody(), variables);

        log.info("======== NOTIFICATION [{}] ========", channel);
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("====================================");
    }

    @Override
    public void send(Collection<NotificationCommand> commands) {
        for (NotificationCommand command : commands) {
            send(command.getType(), command.getChannel(), command.getRecipient(), command.getVariables());
        }
    }
}