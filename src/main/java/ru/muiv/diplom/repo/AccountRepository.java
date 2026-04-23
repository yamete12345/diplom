package ru.muiv.diplom.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.muiv.diplom.domain.Account;
import ru.muiv.diplom.domain.AccountStatus;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdOrderByOpenedAtDesc(Long userId);
    List<Account> findByUserIdAndStatusOrderByOpenedAtDesc(Long userId, AccountStatus status);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
