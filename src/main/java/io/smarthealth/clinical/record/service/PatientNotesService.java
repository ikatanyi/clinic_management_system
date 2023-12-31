/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.record.service;

import io.smarthealth.administration.config.domain.GlobalConfiguration;
import io.smarthealth.administration.config.service.ConfigService;
import io.smarthealth.clinical.record.data.PatientNotesData;
import io.smarthealth.clinical.record.domain.PatientNotes;
import io.smarthealth.clinical.record.domain.PatientNotesRepository;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.facility.service.EmployeeService;
import io.smarthealth.organization.person.domain.PersonRepository;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.organization.person.service.PersonService;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Simon.waweru
 */
@Service
public class PatientNotesService {

    @Autowired
    PatientNotesRepository patientNotesRepository;

    @Autowired
    PatientService patientService;

    @Autowired
    EmployeeService employeeService;

    @Autowired
    PersonService personService;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private ConfigService configService;

    /*
    
    a. Create new patient note
    b. Read all patient notes
    c. Read patient notes by patient number
    d. Read patient notes by doctor and by patient number
    e. Read patient notes by visit
    f. Update patient note
    g. Delete patient note
   
    
     */
    //a Create new patient note
    @Transactional
    public PatientNotes createPatientNote(PatientNotes patientNotes) {
        return patientNotesRepository.save(patientNotes);
    }

    //b. Read all patient notes
    public Page<PatientNotes> fetchAllPatientNotes(final Pageable pageable) {
        return patientNotesRepository.findAll(pageable);
    }

    //c. Read all patient notes by patient number
    public Page<PatientNotes> fetchAllPatientNotesByPatient(final Patient patient, final Pageable pageable) {
        return patientNotesRepository.findByPatientOrderByDateRecordedDesc(patient, pageable);
    }

    //d. Read patient notes by doctor and by patient number
    public Page<PatientNotes> fetchAllPatientsNotesByDoctorAndPatient(final String doctorNumber, final String patientNo, final Pageable pageable) {
        Patient patient = patientService.findPatientOrThrow(patientNo);
        Employee employee = employeeService.fetchEmployeeByNumberOrThrow(doctorNumber);
        return patientNotesRepository.findByHealthProviderAndPatient(employee, patient, pageable);
    }

    //e. Read patient notes by visit
    public Optional<PatientNotes> fetchPatientNotesByVisit(Visit visit) {
        return patientNotesRepository.findByVisit(visit);
    }

    //e. Read patient notes by visit
    public Page<PatientNotes> fetchAllPatientNotesByVisit(Visit visit, Pageable pgbl) {
        return patientNotesRepository.findByVisit(visit, pgbl);
    }

    public PatientNotes convertDataToEntity(PatientNotesData patientNotesData) {
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        PatientNotes patientNotes = modelMapper.map(patientNotesData, PatientNotes.class);
        return patientNotes;
    }

    public PatientNotesData convertEntityToData(PatientNotes patientNotes) {
        PatientNotesData patientNotesData = new PatientNotesData();//modelMapper.map(patientNotes, PatientNotesData.class);
        patientNotesData.setChiefComplaint(patientNotes.getChiefComplaint());
        patientNotesData.setExaminationNotes(patientNotes.getExaminationNotes());
        patientNotesData.setHistoryNotes(patientNotes.getHistoryNotes());
        patientNotesData.setSocialHistory(patientNotes.getSocialHistory());
        if (patientNotes.getHealthProvider() != null) {
            patientNotesData.setHealthProvider(patientNotes.getHealthProvider().getName());
        }
        patientNotesData.setDateRecorded(patientNotes.getDateRecorded());
        patientNotesData.setVisitNumber(patientNotes.getVisit().getVisitNumber());
        patientNotesData.setVisitStartDate(patientNotes.getVisit().getStartDatetime());
        return patientNotesData;
    }

    public Boolean ValidateConsultationFields(PatientNotesData data) {
        Boolean status = true;
        String[] configs = {"ChiefComplaints", "ExaminationNotes", "SocialNotes", "HistoryNotes"};
        for (int i = 0; i < configs.length; i++) {
            Optional<GlobalConfiguration> config = configService.findByName(configs[i]);
            if (config.isPresent()) {
                switch (config.get().getName()) {
                    case "ChiefComplaints":
                        if (config.get().getValue().equals("1") && data.getChiefComplaint() == null)
                            throw APIException.badRequest("Chief complaints is required");                        
                        break;
                    case "ExaminationNotes":
                        if (config.get().getValue().equals("1") && data.getExaminationNotes() == null)
                            throw APIException.badRequest("Examination is required");
                         break;
                    case "SocialNotes":
                        if (config.get().getValue().equals("1") && data.getSocialHistory() == null)
                            throw APIException.badRequest("Social notes is required");
                        break;
                    case "HistoryNotes":
                        if (config.get().getValue().equals("1") && data.getHistoryNotes() == null) 
                            throw APIException.badRequest("History notes is required");
                         break;
                }
            }

        };           
        return status;
    }

}
