package ru.muiv.diplom.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.muiv.diplom.domain.BankTransaction;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
}
