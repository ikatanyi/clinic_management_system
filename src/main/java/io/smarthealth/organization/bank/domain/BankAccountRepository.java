package io.smarthealth.organization.bank.domain;

import io.smarthealth.accounting.acc.domain.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long>, JpaSpecificationExecutor<BankAccount> {

//    Optional<BankAccount> findByBankName(String bankName);
}
