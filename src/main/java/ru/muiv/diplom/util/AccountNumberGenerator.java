package ru.muiv.diplom.util;

import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.AccountType;
import ru.muiv.diplom.repo.AccountRepository;

import java.security.SecureRandom;

/**
 * Генерирует 20-значный номер счёта. Формат упрощён для учебного проекта:
 * первые 5 знаков — условный «балансовый счёт» (40817 — физлица RUB, 42301 — вклад),
 * далее 3 знака валюты (643 = RUB), 1 контрольный + 11 случайных.
 */
@Component
public class AccountNumberGenerator {

    private static final String RUB_CODE = "643";
    private static final String BALANCE_CHECKING = "40817";
    private static final String BALANCE_SAVINGS  = "42301";

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generate(AccountType type) {
        String prefix = switch (type) {
            case CHECKING -> BALANCE_CHECKING;
            case SAVINGS  -> BALANCE_SAVINGS;
        };
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + RUB_CODE + randomDigits(12);
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать уникальный номер счёта");
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
