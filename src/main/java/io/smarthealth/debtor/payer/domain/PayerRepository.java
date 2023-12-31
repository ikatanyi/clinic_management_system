package io.smarthealth.debtor.payer.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Kelsas
 */
@Repository
public interface PayerRepository extends JpaRepository<Payer, Long>, JpaSpecificationExecutor<Payer>, PayerStatementRepository {

    Payer findByPayerName(String payerName);
    Optional<Payer> findByPayerCode(String payerCode);
}
