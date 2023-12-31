package io.smarthealth.accounting.payment.domain.repository;

import io.smarthealth.accounting.cashier.data.CashierShift;
import io.smarthealth.accounting.payment.domain.Receipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author Kelsas
 */
public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {

    @Modifying
    @Query("UPDATE Receipt p SET p.voided=true, p.voidedDatetime=CURRENT_TIMESTAMP, p.voidedBy=:user, p.comments = :comments WHERE p.id=:id")
    int voidPayment(@Param("user") String user, @Param("id") Long id, String comments);

    Optional<Receipt> findByReceiptNo(String receiptNo);
}
