/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.visit.api;

import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.clinical.queue.data.PatientQueueData;
import io.smarthealth.clinical.queue.domain.PatientQueue;
import io.smarthealth.clinical.queue.service.PatientQueueService;
import io.smarthealth.clinical.record.data.VitalRecordData;
import io.smarthealth.clinical.record.domain.VitalsRecord;
import io.smarthealth.clinical.record.service.TriageService;
import io.smarthealth.clinical.visit.data.VisitData;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.infrastructure.common.ApiResponse;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.sequence.SequenceType;
import io.smarthealth.infrastructure.sequence.service.SequenceService;
import io.smarthealth.organization.facility.domain.Department;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.facility.service.DepartmentService;
import io.smarthealth.organization.facility.service.EmployeeService;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author Kelsas
 */
@RestController
@RequestMapping("/api")
@Api(value = "Patient Visit", description = "Operations pertaining to patient visit in a health facility")
public class ClinicalVisitController {

    @Autowired
    private VisitService visitService;
    @Autowired
    private DepartmentService departmentService;

    @Autowired
    PatientService patientService;
    @Autowired
    TriageService triageService;

    @Autowired
    PatientQueueService patientQueueService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    ServicePointService servicePointService;

    @PostMapping("/visits")
    @ApiOperation(value = "Submit a new patient visit", response = VisitData.class)
    public @ResponseBody
    ResponseEntity<?> addVisitRecord(@RequestBody @Valid final VisitData visitData) {

        Patient patient = patientService.findPatientOrThrow(visitData.getPatientNumber());
        //check if patient has an active visit
        if (visitService.isPatientVisitActive(patient)) {
            throw APIException.conflict("Patient identified by {0} already has an active visit", patient.getPatientNumber());
        }
        Employee employee = null;
        if (visitData.getPractitionerCode() != null || !visitData.getPractitionerCode().equals("")) {
            employee = employeeService.fetchEmployeeByNumberOrThrow(visitData.getPractitionerCode());
        }
        ServicePoint servicePoint = servicePointService.getServicePoint(visitData.getServicePointIdentifier());

        System.out.println("Selected service point " + servicePoint.getName());
        Visit visit = VisitData.map(visitData);
        //generate visit number
        visit.setVisitNumber(sequenceService.nextNumber(SequenceType.VisitNumber)/*String.valueOf(visitService.generateVisitNumber())*/);
        visit.setStartDatetime(visitData.getStartDatetime());
        visit.setPatient(patient);
        visit.setServicePoint(servicePoint);
        visit.setHealthProvider(employee);
        visit = this.visitService.createAVisit(visit);
        //Push it to queue
        PatientQueue patientQueue = new PatientQueue();
        patientQueue.setServicePoint(servicePoint);
        patientQueue.setPatient(patient);
        patientQueue.setStatus(true);
        patientQueue.setVisit(visit);
        patientQueueService.createPatientQueue(patientQueue);
        //Convert to data
        VisitData visitDat = modelMapper.map(visit, VisitData.class);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/visits/{visitNumber}")
                .buildAndExpand(visit.getVisitNumber()).toUri();

        return ResponseEntity.created(location).body(ApiResponse.successMessage("Visit was activated successfully", HttpStatus.CREATED, visitDat));
    }

    @PutMapping("/visits/{visitNumber}")
    @ApiOperation(value = "Update patient visit record", response = VisitData.class)
    public @ResponseBody
    ResponseEntity<?> updateVisitRecord(@PathVariable("visitNumber") final String visitNumber, @RequestBody @Valid final VisitData visitData) {
        Patient patient = patientService.findPatientOrThrow(visitData.getPatientNumber());
        Visit visit = visitService.findVisitEntityOrThrow(visitNumber);
        visit.setScheduled(visitData.getScheduled());
        visit.setStartDatetime(visitData.getStartDatetime());
        visit.setStopDatetime(visitData.getStopDatetime());
        visit.setVisitNumber(visitData.getVisitNumber());
        visit.setVisitType(visitData.getVisitType());
        visit.setStatus(visitData.getStatus());
        visit.setPatient(patient);
        visit = this.visitService.createAVisit(visit);
        //Convert to data
        VisitData visitDat = modelMapper.map(visit, VisitData.class);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/visits/{visitNumber}")
                .buildAndExpand(visit.getVisitNumber()).toUri();

        return ResponseEntity.created(location).body(visitDat);
    }

    @GetMapping("/visits")
    public ResponseEntity<List<VisitData>> fetchAllVisits(@RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {

        Page<VisitData> page = visitService.fetchAllVisits(pageable).map(v -> convertToVisitData(v));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/patients/{id}/visits")
    public ResponseEntity<List<VisitData>> fetchAllVisitsByPatient(@PathVariable("id") final String patientNumber, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {
        System.out.println("patientNumber " + patientNumber);
        Page<VisitData> page = visitService.fetchVisitByPatientNumber(patientNumber, pageable).map(v -> convertToVisitData(v));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/visits/{visitNumber}/vitals")
    @ApiOperation(value = "Create/Add a new patient vital by visit number", response = VitalRecordData.class)
    public @ResponseBody
    ResponseEntity<VitalRecordData> addVitalRecordByVisit(@PathVariable("visitNumber") String visitNumber, @RequestBody @Valid final VitalRecordData vital) {
        VitalsRecord vitalR = this.triageService.addVitalRecordsByVisit(visitNumber, vital);

        VitalRecordData vr = modelMapper.map(vitalR, VitalRecordData.class);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/visits/{visitNumber}/vitals/{id}")
                .buildAndExpand(visitNumber, vitalR.getId()).toUri();

        return ResponseEntity.created(location).body(vr);
    }

    @PostMapping("/patient/{patientNo}/vitals")
    @ApiOperation(value = "", response = VitalRecordData.class)
    public @ResponseBody
    ResponseEntity<VitalRecordData> addVitalRecordByPatient(@PathVariable("patientNo") String patientNo, @RequestBody @Valid final VitalRecordData vital) {
        Patient patient = patientService.findPatientOrThrow(patientNo);

        VitalsRecord vitalR = this.triageService.addVitalRecordsByPatient(patient, vital);
        //log queue
        PatientQueue patientQueue = new PatientQueue(); //patientQueueService.fetchQueueByVisitNumber(vitalR.getVisit());
        patientQueue.setUrgency(PatientQueue.QueueUrgency.valueOf(vital.getUrgency()));
        patientQueue.setPatient(patient);
        patientQueue.setVisit(vitalR.getVisit());
        if (vital.getSendTo().equals("specialist")) {
            Employee employee = employeeService.fetchEmployeeByNumberOrThrow(vital.getStaffNumber());
            patientQueue.setStaffNumber(employee);
            patientQueue.setServicePoint(employee.getDepartment().getServicePointType());
            patientQueue.setSpecialNotes("Sent from triage");
            vitalR.getVisit().setServicePoint(employee.getDepartment().getServicePointType());
            vitalR.getVisit().setHealthProvider(employee);
        } else if (vital.getSendTo().equals("Service Point")) {
            ServicePoint servicePoint = servicePointService.getServicePoint(vital.getServicePointIdentifier());
            if (servicePoint.getServicePointType().equals(ServicePointType.Triage)) {
                throw APIException.conflict("Please select another service point. Patient is already on {0}", ServicePointType.Triage.name());
            }
            patientQueue.setServicePoint(servicePoint);
            patientQueue.setSpecialNotes("Sent from triage");
            vitalR.getVisit().setServicePoint(servicePoint);
        } else {
            //patientQueue.setStatus(false);
        }
        visitService.createAVisit(vitalR.getVisit());
        patientQueueService.createPatientQueue(patientQueue);
        VitalRecordData vr = modelMapper.map(vitalR, VitalRecordData.class);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/patient/{patientNo}/vitals/{id}")
                .buildAndExpand(patientNo, vitalR.getId()).toUri();

        return ResponseEntity.created(location).body(vr);
    }

    @GetMapping("/visits/{visitNumber}/vitals")
    @ApiOperation(value = "Fetch all patient vitals by visits", response = VitalRecordData.class)
    public ResponseEntity<List<VitalRecordData>> fetchAllVitalsByVisit(@PathVariable("visitNumber") final String visitNumber, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {
        Page<VitalRecordData> page = triageService.fetchVitalRecordsByVisit(visitNumber, pageable).map(v -> convertToVitalsData(v));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/patients/{patientNumber}/vitals")
    @ApiOperation(value = "Fetch all patient vitals by patient", response = VitalRecordData.class)
    public ResponseEntity<List<VitalRecordData>> fetchAllVitalsByPatient(@PathVariable("patientNumber") final String patientNumber, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {

        Page<VitalRecordData> page = triageService.fetchVitalRecordsByPatient(patientNumber, pageable).map(v -> convertToVitalsData(v));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    private VisitData convertToVisitData(Visit visit) {
        VisitData visitData = modelMapper.map(visit, VisitData.class);
        if (visit.getServicePoint() != null) {
            visitData.setServicePointIdentifier(visit.getServicePoint().getId());
            visitData.setServicePointName(visit.getServicePoint().getName());
        }
        Patient patient = patientService.findPatientOrThrow(visitData.getPatientNumber());
        visitData.setPatientData(patientService.convertToPatientData(patient));
        return visitData;
    }

    private VitalRecordData convertToVitalsData(VitalsRecord vitalsRecord) {
        return modelMapper.map(vitalsRecord, VitalRecordData.class);
    }

}
