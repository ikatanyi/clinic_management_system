package io.smarthealth.accounting.acc.domain;

import io.smarthealth.accounting.acc.data.v1.FinancialActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author Kelsas
 */
public interface FinancialActivityAccountRepository extends JpaRepository<FinancialActivityAccount, Long> {

    Optional<FinancialActivityAccount> findByAccount(AccountEntity account);
    
    Optional<FinancialActivityAccount> findByFinancialActivity(FinancialActivity activity);
    
}
