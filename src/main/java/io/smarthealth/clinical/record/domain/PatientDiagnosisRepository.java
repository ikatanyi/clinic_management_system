/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.record.domain;

import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.organization.person.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 *
 * @author Simon.waweru
 */
public interface PatientDiagnosisRepository extends JpaRepository<PatientDiagnosis, Long>, JpaSpecificationExecutor<PatientDiagnosis> {

    Page<PatientDiagnosis> findByPatient(final Patient patient, Pageable page);

    Page<PatientDiagnosis> findByVisit(Visit visit, Pageable pageable);

    @Query("SELECT d FROM PatientDiagnosis  d WHERE d.visit.visitNumber = :visitNumber")
    List<PatientDiagnosis> findPatientDiagnosisByVisitNumber(String visitNumber);

    @Modifying
    @Query("DELETE FROM PatientDiagnosis  p where p.id = :id")
    void deleteDiagnosis(Long id);
}
