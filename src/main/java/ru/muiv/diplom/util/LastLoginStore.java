package ru.muiv.diplom.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Сохраняет e-mail последнего успешно вошедшего пользователя в data/last-login.properties,
 * чтобы предзаполнять поле на экране входа. Пароль не сохраняется — OTP на почту всё равно нужен.
 */
@Component
public class LastLoginStore {

    private static final Path FILE = Path.of("data", "last-login.properties");
    private static final String KEY = "email";

    public String load() {
        try {
            if (!Files.exists(FILE)) return "";
            Properties p = new Properties();
            try (var in = Files.newInputStream(FILE)) {
                p.load(in);
            }
            return p.getProperty(KEY, "");
        } catch (IOException e) {
            return "";
        }
    }

    public void save(String email) {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty(KEY, email == null ? "" : email);
            try (var out = Files.newOutputStream(FILE)) {
                p.store(out, "last successful login");
            }
        } catch (IOException ignored) {
        }
    }
}
