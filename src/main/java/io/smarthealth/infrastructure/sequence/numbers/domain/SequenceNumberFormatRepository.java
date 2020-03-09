package io.smarthealth.infrastructure.sequence.numbers.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Kelsas
 */
@Deprecated
public interface SequenceNumberFormatRepository extends JpaRepository<SequenceNumberFormat, Long> {

    Optional<SequenceNumberFormat> findBySequenceType(EntitySequenceType sequenceType);

    Page<SequenceNumberFormat> findBySequenceType(EntitySequenceType sequenceType, Pageable page);
}