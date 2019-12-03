/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.visit.data;

import io.smarthealth.clinical.record.domain.Diagnosis;
import io.smarthealth.clinical.visit.domain.Admission;
import io.smarthealth.organization.facility.data.EmployeeData;
import java.util.List;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Simon.waweru
 */
@Data
public class AdmissionData {

    @Enumerated(EnumType.STRING)
    private Admission.Type admissionType;
    
    private Long diagnosisId;
    
    private Long employeeId;
    
    private String visitNumber;
    
    List<Diagnosis> diagnosis;

    private EmployeeData employee;
    
    private VisitData visit;

}
