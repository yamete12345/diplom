package ru.muiv.diplom.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.muiv.diplom.domain.OtpPurpose;
import ru.muiv.diplom.domain.User;
import ru.muiv.diplom.repo.UserRepository;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PendingRegistrationService pendingRegistrations;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       EmailService emailService,
                       PendingRegistrationService pendingRegistrations) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.pendingRegistrations = pendingRegistrations;
    }

    /**
     * Регистрация (шаг 1): НЕ сохраняет пользователя в БД, а кладёт заявку в in-memory
     * хранилище и шлёт OTP на почту. Возвращает токен заявки для последующего подтверждения.
     */
    public String register(String fullName, String email, String rawPassword) {
        validate(fullName, email, rawPassword);
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Пользователь с таким e-mail уже зарегистрирован");
        }
        // Если есть незавершённая заявка на этот e-mail — отменяем её (перезапись).
        pendingRegistrations.removeByEmail(normalizedEmail);

        String passwordHash = passwordEncoder.encode(rawPassword);
        var pending = pendingRegistrations.create(fullName.trim(), normalizedEmail, passwordHash);

        try {
            emailService.sendOtp(normalizedEmail, pending.code(), "подтверждение регистрации");
        } catch (RuntimeException e) {
            // Письмо не ушло → сбрасываем pending, чтобы не висел мусор.
            pendingRegistrations.cancel(pending.token());
            throw e;
        }
        return pending.token();
    }

    /**
     * Регистрация (шаг 2): проверяет OTP. Только при успехе создаётся запись в БД.
     * Возвращает созданного пользователя или null, если код неверный/просрочен.
     */
    @Transactional
    public User confirmRegistration(String pendingToken, String code) {
        var pending = pendingRegistrations.consumeIfValid(pendingToken, code);
        if (pending == null) {
            return null;
        }
        // Двойная проверка на случай, если кто-то успел зарегистрироваться с тем же e-mail.
        if (userRepository.existsByEmail(pending.email())) {
            throw new IllegalArgumentException("Пользователь с таким e-mail уже зарегистрирован");
        }
        User user = User.builder()
                .email(pending.email())
                .passwordHash(pending.passwordHash())
                .fullName(pending.fullName())
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }

    /** Явная отмена регистрации (пользователь закрыл OTP-окно). */
    public void cancelRegistration(String pendingToken) {
        pendingRegistrations.cancel(pendingToken);
    }

    /**
     * Первый шаг логина: проверяет e-mail + пароль.
     * Возвращает пользователя и шлёт OTP для второго шага.
     */
    @Transactional
    public User loginStepPassword(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("Логин или пароль неверный");
        }
        Optional<User> opt = userRepository.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty() || !passwordEncoder.matches(rawPassword, opt.get().getPasswordHash())) {
            throw new IllegalArgumentException("Логин или пароль неверный");
        }
        User user = opt.get();
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("E-mail не подтверждён. Завершите регистрацию.");
        }
        String code = otpService.issue(user.getId(), OtpPurpose.LOGIN);
        emailService.sendOtp(user.getEmail(), code, "вход в систему");
        return user;
    }

    /** Второй шаг логина: проверка OTP. */
    public boolean loginStepOtp(Long userId, String code) {
        return otpService.verify(userId, OtpPurpose.LOGIN, code);
    }

    /** Инициирует смену пароля: проверяет старый пароль и шлёт OTP. */
    @Transactional
    public void requestPasswordChange(Long userId, String oldPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Текущий пароль неверный");
        }
        String code = otpService.issue(userId, OtpPurpose.PASSWORD_CHANGE);
        emailService.sendOtp(user.getEmail(), code, "смена пароля");
    }

    /** Подтверждение смены пароля OTP. */
    @Transactional
    public boolean confirmPasswordChange(Long userId, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Новый пароль должен быть не короче 6 символов");
        }
        if (!otpService.verify(userId, OtpPurpose.PASSWORD_CHANGE, code)) {
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    private void validate(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("ФИО обязательно");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Некорректный e-mail");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не короче 6 символов");
        }
    }
}
