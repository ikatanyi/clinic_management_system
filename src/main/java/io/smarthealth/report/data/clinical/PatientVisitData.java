/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.report.data.clinical;

import io.smarthealth.clinical.laboratory.data.LabRegisterTestData;
import io.smarthealth.clinical.procedure.data.PatientProcedureTestData;
import io.smarthealth.clinical.radiology.data.PatientScanTestData;
import io.smarthealth.clinical.record.data.DiagnosisData;
import io.smarthealth.clinical.record.data.PrescriptionData;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 *
 * @author Simon.waweru
 */
@Data
public class PatientVisitData {
   
    private String fullName;
    private String visitNumber;
    private String createdOn;
    private String practitionerName;
    
    private String chiefComplaint;
    private String historyNotes; //history of present complaints
    private String examinationNotes;
    private String socialHistory;
    private String briefNotes;
    private Integer age;
    
    List<PatientScanTestData> radiologyTests = new ArrayList();
    List<PatientProcedureTestData> procedures = new ArrayList();
    List<LabRegisterTestData> labTests = new ArrayList();
    List<DiagnosisData> diagnosis = new ArrayList();
    List<PrescriptionData> drugsData = new ArrayList();
    List<EmployeeBanner> employeeData = new ArrayList();
}

