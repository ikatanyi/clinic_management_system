/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.debtor.payer.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Simon.waweru
 */
@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long>, JpaSpecificationExecutor<Scheme> {

    Page<Scheme> findByPayer(Payer payer, Pageable pageable);

    Optional<Scheme> findBySchemeName(String schemeName);

    Optional<Scheme> findSchemeBySchemeNameAndPayer_PayerCode(String schemeName, String payerCode);

    Optional<Scheme> findBySchemeCode(String schemeCode);
}
