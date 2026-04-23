package ru.muiv.diplom.util;

import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.BankTransaction;
import ru.muiv.diplom.domain.Posting;
import ru.muiv.diplom.repo.BankTransactionRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public CsvExporter(BankTransactionRepository txRepository) {
        this.txRepository = txRepository;
    }

    public void exportHistory(List<Posting> postings, Path target) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            // BOM для корректного открытия в Excel
            w.write('\ufeff');
            w.write("Дата;Тип;Направление;Сумма;Описание");
            w.newLine();
            for (Posting p : postings) {
                Optional<BankTransaction> txOpt = txRepository.findById(p.getTransactionId());
                if (txOpt.isEmpty()) continue;
                BankTransaction tx = txOpt.get();
                w.write(String.join(";",
                        DATE_FMT.format(tx.getCreatedAt()),
                        tx.getType().getTitle(),
                        p.getDirection().name(),
                        p.getAmount().toPlainString(),
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
