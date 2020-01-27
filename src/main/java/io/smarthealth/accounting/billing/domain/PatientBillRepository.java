package io.smarthealth.accounting.billing.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author Kennedy.Imbenzi
 */
public interface PatientBillRepository extends JpaRepository<PatientBill, Long>, JpaSpecificationExecutor<PatientBill> {
    
    Optional<PatientBill> findByBillNumber(final String identifier);
     
}