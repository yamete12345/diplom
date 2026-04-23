package ru.muiv.diplom.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.stub:false}")
    private boolean stub;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String code, String purposeTitle) {
        String subject = "Код подтверждения: " + purposeTitle;
        String text = "Здравствуйте!\n\n"
                + "Ваш код подтверждения: " + code + "\n"
                + "Код действителен 5 минут.\n\n"
                + "Если вы не запрашивали код — проигнорируйте письмо.\n";

        if (stub) {
            log.warn("[MAIL STUB] На адрес {} должен был уйти OTP: {} (цель: {})", to, code, purposeTitle);
            return;
        }

        String sender = (from != null && !from.isBlank()) ? from : smtpUsername;
        if (sender == null || sender.isBlank()) {
            throw new IllegalStateException(
                    "Не заданы SMTP-учётные данные. Установите переменные окружения "
                            + "MAIL_USERNAME и MAIL_PASSWORD (см. README).");
        }

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(mime);
            log.info("OTP отправлен на {} (цель: {})", to, purposeTitle);
        } catch (Exception e) {
            log.error("Не удалось отправить OTP на {}: {}", to, e.getMessage());
            throw new IllegalStateException(
                    "Не удалось отправить письмо с кодом: " + e.getMessage(), e);
        }
    }
}
