package ru.muiv.diplom.util;

import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.Account;
import ru.muiv.diplom.domain.BankTransaction;
import ru.muiv.diplom.domain.Posting;
import ru.muiv.diplom.domain.TransactionType;
import ru.muiv.diplom.domain.User;
import ru.muiv.diplom.repo.AccountRepository;
import ru.muiv.diplom.repo.BankTransactionRepository;
import ru.muiv.diplom.repo.PostingRepository;
import ru.muiv.diplom.repo.UserRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class CsvExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BankTransactionRepository txRepository;
    private final PostingRepository postingRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public CsvExporter(BankTransactionRepository txRepository,
                       PostingRepository postingRepository,
                       AccountRepository accountRepository,
                       UserRepository userRepository) {
        this.txRepository = txRepository;
        this.postingRepository = postingRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public void exportHistory(List<Posting> postings, Path target) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            // BOM для корректного открытия в Excel
            w.write('\ufeff');
            w.write("Дата;Тип;Направление;Сумма;Контрагент (ФИО);Счёт контрагента;Описание");
            w.newLine();
            for (Posting p : postings) {
                Optional<BankTransaction> txOpt = txRepository.findById(p.getTransactionId());
                if (txOpt.isEmpty()) continue;
                BankTransaction tx = txOpt.get();
                String cpName = "";
                String cpAccount = "";
                if (tx.getType() == TransactionType.TRANSFER) {
                    Posting other = postingRepository.findByTransactionId(tx.getId()).stream()
                            .filter(x -> !x.getId().equals(p.getId()))
                            .findFirst().orElse(null);
                    if (other != null) {
                        Account otherAcc = accountRepository.findById(other.getAccountId()).orElse(null);
                        if (otherAcc != null) {
                            cpAccount = otherAcc.getAccountNumber();
                            cpName = userRepository.findById(otherAcc.getUserId())
                                    .map(User::getFullName).orElse("");
                        }
                    }
                }
                w.write(String.join(";",
                        DATE_FMT.format(tx.getCreatedAt()),
                        tx.getType().getTitle(),
                        p.getDirection().name(),
                        p.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        csv(cpName),
                        cpAccount,
                        csv(tx.getDescription())
                ));
                w.newLine();
            }
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}
