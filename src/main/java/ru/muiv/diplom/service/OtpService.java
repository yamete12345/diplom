package ru.muiv.diplom.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.muiv.diplom.domain.OtpCode;
import ru.muiv.diplom.domain.OtpPurpose;
import ru.muiv.diplom.repo.OtpCodeRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class OtpService {

    private final OtpCodeRepository otpRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int length;

    @Value("${app.otp.ttl-minutes:5}")
    private int ttlMinutes;

    public OtpService(OtpCodeRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /** Создаёт и сохраняет OTP (хешом), возвращает plain-код для отправки пользователю. */
    @Transactional
    public String issue(Long userId, OtpPurpose purpose) {
        // гасим предыдущие незакрытые коды той же цели
        List<OtpCode> active = otpRepository
                .findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose);
        active.forEach(c -> c.setUsed(true));
        otpRepository.saveAll(active);

        String code = generateCode();
        OtpCode entity = OtpCode.builder()
                .userId(userId)
                .purpose(purpose)
                .codeHash(sha256(code))
                .expiresAt(Instant.now().plusSeconds(ttlMinutes * 60L))
                .used(false)
                .build();
        otpRepository.save(entity);
        return code;
    }

    /** Возвращает true и помечает код использованным, если он валиден. */
    @Transactional
    public boolean verify(Long userId, OtpPurpose purpose, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String hash = sha256(code.trim());
        Instant now = Instant.now();
        List<OtpCode> candidates = otpRepository
                .findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose);
        for (OtpCode otp : candidates) {
            if (otp.getExpiresAt().isAfter(now) && otp.getCodeHash().equals(hash)) {
                otp.setUsed(true);
                otpRepository.save(otp);
                return true;
            }
        }
        return false;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
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
}
