package com.reportplatform.notif.service;

import com.reportplatform.notif.dto.NotificationEvent;
import com.reportplatform.notif.dto.NotificationResponse;
import com.reportplatform.notif.model.NotificationSettingsEntity;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;

/**
 * Service for sending email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationSettingsRepository settingsRepository;

    @Value("${app.mail.from:noreply@reportplatform.local}")
    private String mailFrom;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Send email notification based on settings.
     */
    @Async
    public void sendEmailNotification(NotificationEvent event, NotificationResponse notification) {
        if (!mailEnabled) {
            log.debug("Email notifications disabled, skipping email for {}", notification.getType());
            return;
        }

        String userId = event.getTargetUserId();
        String orgId = event.getData() != null ? event.getData().get("orgId") : null;

        if (orgId == null) {
            log.warn("No orgId found for email notification, skipping");
            return;
        }

        // Check if email is enabled for this notification type
        Optional<NotificationSettingsEntity> settings = settingsRepository
                .findByUserIdAndOrgIdAndNotificationType(userId, orgId, notification.getType());

        boolean emailEnabled = settings.map(NotificationSettingsEntity::getEmailEnabled).orElse(true);
        boolean criticalOnly = settings.map(NotificationSettingsEntity::getCriticalOnly).orElse(false);

        // For non-critical types when criticalOnly is enabled, skip email
        if (criticalOnly && !isCriticalType(notification.getType())) {
            log.debug("Skipping non-critical email for type {} (criticalOnly=true)", notification.getType());
            return;
        }

        if (!emailEnabled) {
            log.debug("Email disabled for notification type {}", notification.getType());
            return;
        }

        // Get user email from event data
        String userEmail = event.getData() != null ? event.getData().get("userEmail") : null;
        if (userEmail == null) {
            log.warn("No user email found for notification {}", notification.getId());
            return;
        }

        sendEmail(userEmail, notification);
    }

    /**
     * Send email to user.
     */
    private void sendEmail(String to, NotificationResponse notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(notification.getTitle());

            // Select template based on notification type
            String templateName = getTemplateName(notification.getType());
            Context context = new Context();
            context.setVariable("notification", notification);
            context.setVariable("title", notification.getTitle());
            context.setVariable("body", notification.getBody());
            context.setVariable("type", notification.getType());
            if (notification.getData() != null) {
                context.setVariable("data", notification.getData());
            }

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} for notification {}", to, notification.getId());
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Get template name based on notification type.
     */
    private String getTemplateName(NotificationType type) {
        return switch (type) {
            case FILE_PROCESSED -> "email/file-processed";
            case FILE_FAILED -> "email/file-failed";
            case REPORT_SUBMITTED -> "email/report-submitted";
            case REPORT_APPROVED -> "email/report-approved";
            case REPORT_REJECTED -> "email/report-rejected";
            case DEADLINE_APPROACHING -> "email/deadline-approaching";
            case DEADLINE_MISSED -> "email/deadline-missed";
            case BATCH_COMPLETED -> "email/batch-completed";
            default -> "email/default";
        };
    }

    /**
     * Check if notification type is critical (always sent even in criticalOnly
     * mode).
     */
    private boolean isCriticalType(NotificationType type) {
        return type == NotificationType.FILE_FAILED ||
                type == NotificationType.REPORT_REJECTED ||
                type == NotificationType.DEADLINE_MISSED;
    }
}
