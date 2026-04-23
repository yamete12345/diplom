package ru.muiv.diplom.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.muiv.diplom.domain.OtpCode;
import ru.muiv.diplom.domain.OtpPurpose;

import java.util.List;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    List<OtpCode> findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
