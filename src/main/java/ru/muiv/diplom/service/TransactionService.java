package ru.muiv.diplom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.muiv.diplom.domain.*;
import ru.muiv.diplom.repo.AccountRepository;
import ru.muiv.diplom.repo.BankTransactionRepository;
import ru.muiv.diplom.repo.PostingRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository txRepository;
    private final PostingRepository postingRepository;

    public TransactionService(AccountRepository accountRepository,
                              BankTransactionRepository txRepository,
                              PostingRepository postingRepository) {
        this.accountRepository = accountRepository;
        this.txRepository = txRepository;
        this.postingRepository = postingRepository;
    }

    /** Пополнение: +баланс счёта, CREDIT-проводка. */
    @Transactional
    public BankTransaction deposit(Long userId, Long accountId, BigDecimal amount, String description) {
        validateAmount(amount);
        Account account = requireOwnActive(userId, accountId);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        BankTransaction tx = txRepository.save(BankTransaction.builder()
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .currency(account.getCurrency())
                .description(description)
                .build());

        postingRepository.save(Posting.builder()
                .transactionId(tx.getId())
                .accountId(account.getId())
                .direction(PostingDirection.CREDIT)
                .amount(amount)
                .build());
        return tx;
    }

    /** Списание: −баланс счёта, DEBIT-проводка. */
    @Transactional
    public BankTransaction withdraw(Long userId, Long accountId, BigDecimal amount, String description) {
        validateAmount(amount);
        Account account = requireOwnActive(userId, accountId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств на счёте");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        BankTransaction tx = txRepository.save(BankTransaction.builder()
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .currency(account.getCurrency())
                .description(description)
                .build());

        postingRepository.save(Posting.builder()
                .transactionId(tx.getId())
                .accountId(account.getId())
                .direction(PostingDirection.DEBIT)
                .amount(amount)
                .build());
        return tx;
    }

    /**
     * Перевод: две проводки (DEBIT с отправителя, CREDIT получателю) в одной БД-транзакции.
     * Получатель ищется по номеру счёта. Счета могут быть в разных валютах только одной — RUB.
     */
    @Transactional
    public BankTransaction transfer(Long userId,
                                    Long fromAccountId,
                                    String toAccountNumber,
                                    BigDecimal amount,
                                    String description) {
        validateAmount(amount);
        Account from = requireOwnActive(userId, fromAccountId);
        Account to = accountRepository.findByAccountNumber(toAccountNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Счёт получателя не найден"));

        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Счёт получателя закрыт");
        }
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Нельзя перевести на тот же счёт");
        }
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new IllegalStateException("Валюты счетов не совпадают");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств на счёте");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        BankTransaction tx = txRepository.save(BankTransaction.builder()
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .currency(from.getCurrency())
                .description(description)
                .build());

        postingRepository.save(Posting.builder()
                .transactionId(tx.getId())
                .accountId(from.getId())
                .direction(PostingDirection.DEBIT)
                .amount(amount)
                .build());
        postingRepository.save(Posting.builder()
                .transactionId(tx.getId())
                .accountId(to.getId())
                .direction(PostingDirection.CREDIT)
                .amount(amount)
                .build());
        return tx;
    }

    /** История операций по счёту за период. */
    public List<Posting> history(Long userId, Long accountId, Instant from, Instant to) {
        requireOwn(userId, accountId);
        return postingRepository.findByAccountAndPeriod(accountId, from, to);
    }

    private Account requireOwn(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Счёт не найден"));
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Счёт принадлежит другому пользователю");
        }
        return account;
    }

    private Account requireOwnActive(Long userId, Long accountId) {
        Account account = requireOwn(userId, accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Счёт закрыт");
        }
        return account;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
    }
}
