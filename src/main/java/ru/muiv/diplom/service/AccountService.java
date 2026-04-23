package ru.muiv.diplom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.muiv.diplom.domain.Account;
import ru.muiv.diplom.domain.AccountStatus;
import ru.muiv.diplom.domain.AccountType;
import ru.muiv.diplom.repo.AccountRepository;
import ru.muiv.diplom.util.AccountNumberGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator numberGenerator;

    public AccountService(AccountRepository accountRepository,
                          AccountNumberGenerator numberGenerator) {
        this.accountRepository = accountRepository;
        this.numberGenerator = numberGenerator;
    }

    public List<Account> listForUser(Long userId) {
        return accountRepository.findByUserIdOrderByOpenedAtDesc(userId);
    }

    public List<Account> listActiveForUser(Long userId) {
        return accountRepository
                .findByUserIdAndStatusOrderByOpenedAtDesc(userId, AccountStatus.ACTIVE);
    }

    @Transactional
    public Account open(Long userId, AccountType type) {
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(numberGenerator.generate(type))
                .type(type)
                .currency("RUB")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    @Transactional
    public void close(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Счёт не найден"));
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Счёт принадлежит другому пользователю");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Счёт уже закрыт");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Нельзя закрыть счёт с ненулевым балансом");
        }
        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(Instant.now());
        accountRepository.save(account);
    }
}
