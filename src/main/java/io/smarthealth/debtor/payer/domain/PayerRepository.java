package io.smarthealth.debtor.payer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Kelsas
 */
@Repository
public interface PayerRepository extends JpaRepository<Payer, Long> {

}
