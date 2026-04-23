package ru.muiv.diplom.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "postings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private PostingDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
}
