/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.lab.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 *
 * @author Kennedy.Imbenzi
 */
public interface AnalyteRepository extends JpaRepository<Analyte, Long> {
    
    Page<Analyte> findByTestType(LabTestType testtype, Pageable pageable);
    
    @Query("SELECT e FROM Analyte e WHERE e.testType = :testType AND (e.gender = :gender OR e.gender='Both') AND :age BETWEEN e.startAge and e.endAge")
    Page<Analyte> findAnalytesByGenderAndAge(LabTestType testType, String gender, Integer age, Pageable page);    
}
