package ru.muiv.diplom.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит в памяти заявки на регистрацию, пока пользователь не подтвердил e-mail OTP.
 * Таким образом в БД не попадают «мусорные» записи от незавершённых регистраций.
 * TTL — 10 минут, просроченные заявки вычищаются при каждом обращении.
 */
@Service
public class PendingRegistrationService {

    private static final long TTL_SECONDS = 600;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> byToken = new ConcurrentHashMap<>();

    public PendingRegistration create(String fullName, String email, String passwordHash) {
        purgeExpired();
        String token = UUID.randomUUID().toString();
        String code = generateCode();
        Pending p = new Pending(
                email,
                fullName,
                passwordHash,
                sha256(code),
                Instant.now().plusSeconds(TTL_SECONDS)
        );
        byToken.put(token, p);
        return new PendingRegistration(token, code, email);
    }

    /** Проверяет код. Если валиден — возвращает данные и удаляет заявку из памяти. */
    public Pending consumeIfValid(String token, String code) {
        purgeExpired();
        if (token == null || code == null) return null;
        Pending p = byToken.get(token);
        if (p == null) return null;
        if (p.expiresAt.isBefore(Instant.now())) {
            byToken.remove(token);
            return null;
        }
        if (!p.codeHash.equals(sha256(code.trim()))) {
            return null;
        }
        byToken.remove(token);
        return p;
    }

    /** Явная отмена (пользователь закрыл окно OTP). */
    public void cancel(String token) {
        if (token != null) byToken.remove(token);
    }

    /** Уже есть активная заявка на этот e-mail? (для блокировки дубликатов) */
    public boolean hasPendingForEmail(String email) {
        purgeExpired();
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        return byToken.values().stream()
                .anyMatch(p -> p.email.equals(normalized));
    }

    /** Удалить pending-заявки на e-mail (нужно при повторной регистрации). */
    public void removeByEmail(String email) {
        if (email == null) return;
        String normalized = email.trim().toLowerCase();
        byToken.entrySet().removeIf(e -> e.getValue().email.equals(normalized));
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        byToken.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Pending(String email, String fullName, String passwordHash,
                          String codeHash, Instant expiresAt) {}

    public record PendingRegistration(String token, String code, String email) {}
}
