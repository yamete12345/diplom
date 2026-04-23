package ru.muiv.diplom.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.muiv.diplom.domain.Posting;

import java.time.Instant;
import java.util.List;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    @Query("""
            select p from Posting p
            join BankTransaction t on t.id = p.transactionId
            where p.accountId = :accountId
              and t.createdAt between :from and :to
            order by t.createdAt desc
            """)
    List<Posting> findByAccountAndPeriod(@Param("accountId") Long accountId,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to);

    List<Posting> findByTransactionId(Long transactionId);
}
