/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.visit.domain;

import io.smarthealth.organization.person.patient.domain.Patient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author simz
 */
@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {

    Optional<PaymentDetails> findByVisit(final Visit visit);

    Optional<PaymentDetails> findFirstByPatientOrderByIdDesc(final Patient patient);

    @Query("SELECT p FROM PaymentDetails p where p.visit.visitNumber = :visitId")
    Optional<PaymentDetails> findByVisitNumber(String visitId);
}
