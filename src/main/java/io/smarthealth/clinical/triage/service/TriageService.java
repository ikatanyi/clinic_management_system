package io.smarthealth.clinical.triage.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.smarthealth.clinical.record.data.VitalRecordData;
import io.smarthealth.clinical.record.domain.TriageRepository;
import io.smarthealth.clinical.record.domain.VitalsRecord;
import io.smarthealth.clinical.record.service.BMI;
import io.smarthealth.clinical.triage.domain.ExtraVitalField;
import io.smarthealth.clinical.triage.domain.ExtraVitalFieldRepository;
import io.smarthealth.clinical.triage.domain.ExtraVitalValue;
import io.smarthealth.clinical.triage.domain.ExtraVitalValueRepository;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.VisitRepository;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.utility.ContentPage;
import io.smarthealth.organization.facility.service.EmployeeService;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.domain.PatientRepository;
import io.smarthealth.organization.person.patient.service.PatientService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Kelsas
 */
@Service
public class TriageService {

    @Autowired
    private TriageRepository triageRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private ExtraVitalValueRepository extraVitalValueRepository;

    @Autowired
    private ExtraVitalFieldRepository extraVitalFieldRepository;

    //VITALS
    public VitalsRecord addVitalRecordsByVisit(Visit visit, VitalRecordData triage) {

        if (!StringUtils.equalsIgnoreCase(visit.getPatient().getPatientNumber(), triage.getPatientNumber())) {
            throw APIException.badRequest("Invalid Patient Number! mismatch in Patient Visit's patient number");
        }

        VitalsRecord vr = VitalRecordData.map(triage);

        try {
            float bmi = (float) BMI.calculateBMI(triage.getHeight(), triage.getWeight());
            String category = BMI.getCategory(bmi);
            vr.setCategory(category);
            vr.setBmi(bmi);
        } catch (Exception e) {
            System.out.println("Unable to calculate BMI " + e.getMessage());
        }

        vr.setPatient(visit.getPatient());
        vr.setVisit(visit);

        return triageRepository.save(vr);
    }

    @Transactional
    public VitalsRecord addVitalRecordsByPatient(Patient patient, VitalRecordData triage) throws JSONException {
        //validate visit
        Optional<Visit> visit = visitRepository.findByVisitNumber(triage.getVisitNumber());
        VitalsRecord vr = VitalRecordData.map(triage);
        if (visit.isPresent()) {
            vr.setVisit(visit.get());
        }
        float bmi = 0;
        try {
            bmi = (float) BMI.calculateBMI(triage.getHeight(), triage.getWeight());
        } catch (Exception e) {
            System.out.println("Weight/Height Unavailable");
        }
        String category = BMI.getCategory(bmi);
        vr.setPatient(patient);
        vr.setBmi(bmi);
        vr.setCategory(category);

        VitalsRecord savedVr = triageRepository.save(vr);
        List<ExtraVitalValue> extraVitalValues = new ArrayList<>();
        if (triage.getExtraVitals() != null) {
            JsonNode jsonNode = triage.getExtraVitals();

            System.out.println("Json object"+jsonNode.toString());
            //fetch all extra vital fields
            List<ExtraVitalField> fields = extraVitalFieldRepository.findAll();
            for (ExtraVitalField f : fields
            ) {
                JsonNode fieldNode = jsonNode.get(f.getName());

                if(fieldNode != null && !fieldNode.isNull()) {
                    ExtraVitalValue evv = new ExtraVitalValue();
                    evv.setVitalRecord(savedVr);
                    evv.setField(f);
                    evv.setValue(jsonNode.get(f.getName()).asText());
                    extraVitalValues.add(evv);
                }

            }
        }
        extraVitalValueRepository.saveAll(extraVitalValues);
        return savedVr;
    }

    public Page<VitalsRecord> fetchVitalRecordsByVisit(String visitNumber, Pageable page) {
        Optional<Visit> visit = visitRepository.findByVisitNumber(visitNumber);
        if (visit.isPresent()) {
            return this.triageRepository.findByVisit(visit.get(), page);
        } else {
            throw APIException.notFound("Visit identified by : {0} not found.", visitNumber);
        }
    }

    public Page<VitalsRecord> fetchVitalRecordsByPatient(String patientNumber, Pageable page) {
        Patient patient = this.patientService.findPatientOrThrow(patientNumber);
        return this.triageRepository.findByPatient(patient, page);
    }

    public Optional<VitalsRecord> fetchLastVitalRecordsByPatient(String patientNumber) {
        Patient patient = this.patientService.findPatientOrThrow(patientNumber);
        //Sort.by(Sort.Direction.ASC, "dateRecorded")
        return triageRepository.findFirstByPatientOrderByIdDesc(patient);
    }

    public ContentPage<VitalRecordData> fetchVitalRecords(String patientNumber, Pageable page) {

        Page<VitalsRecord> triageEntities;
        if (patientNumber != null) {
            Patient patient = patientRepository.findByPatientNumber(patientNumber).orElse(null);
            triageEntities = triageRepository.findByPatient(patient, page);
        } else {
            triageEntities = this.triageRepository.findAll(page);
        }

        final ContentPage<VitalRecordData> triagePage = new ContentPage();
        triagePage.setTotalPages(triageEntities.getTotalPages());
        triagePage.setTotalElements(triageEntities.getTotalElements());
        if (triageEntities.getSize() > 0) {
            final ArrayList<VitalRecordData> triagelist = new ArrayList<>(triageEntities.getSize());
            triagePage.setContents(triagelist);
            triageEntities.forEach((vt) -> triagelist.add(VitalRecordData.map(vt)));
        }

        return triagePage;

    }

    public VitalRecordData getVitalRecordsById(String visitNumber, Long id) {
        Optional<VitalsRecord> entity = this.triageRepository.findById(id);

        return entity.map(VitalRecordData::map).orElse(null);
    }

    private Visit findVisitOrThrow(String visitNumber) {
        return this.visitRepository.findByVisitNumber(visitNumber)
                .orElseThrow(() -> APIException.notFound("Patient Session with Visit Number : {0} not found.", visitNumber));
    }

    public VitalRecordData convertToVitalsData(VitalsRecord vitalsRecord) {
        return VitalRecordData.map(vitalsRecord); //modelMapper.map(vitalsRecord, VitalRecordData.class);
    }

}
