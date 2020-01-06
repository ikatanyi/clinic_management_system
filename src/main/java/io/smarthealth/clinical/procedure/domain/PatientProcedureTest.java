/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.procedure.domain;

/**
 *
 * @author Kennedy.Imbenzi
 */


import io.smarthealth.clinical.procedure.domain.enumeration.ProcedureTestState;
import io.smarthealth.clinical.record.domain.ClinicalRecord;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "patient_procedure_tests")
@Inheritance(strategy = InheritanceType.JOINED)
public class PatientProcedureTest extends ClinicalRecord{
    
    @OneToOne
    private ProcedureTest procedureTest;
    private double testPrice;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private ProcedureTestState status;
    private String result;
    private String comments;  
    
    @ManyToOne
    private PatientProcedureRegister patientProcedureRegister;
}
