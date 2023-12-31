package io.smarthealth.organization.person.patient.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.smarthealth.infrastructure.common.ApiResponse;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.organization.person.data.AddressDatas;
import io.smarthealth.organization.person.data.ContactData;
import io.smarthealth.organization.person.data.PersonIdentifierData;
import io.smarthealth.organization.person.data.PersonNextOfKinData;
import io.smarthealth.organization.person.data.PortraitData;
import io.smarthealth.organization.person.domain.PersonAddress;
import io.smarthealth.organization.person.domain.PersonContact;
import io.smarthealth.organization.person.domain.PersonNextOfKin;
import io.smarthealth.organization.person.domain.Portrait;
import io.smarthealth.organization.person.patient.data.AllergyTypeData;
import io.smarthealth.organization.person.patient.data.PatientAllergiesData;
import io.smarthealth.organization.person.patient.data.PatientData;
import io.smarthealth.organization.person.patient.domain.*;
import io.smarthealth.organization.person.patient.service.*;
import io.smarthealth.organization.person.service.PersonService;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

//import jdk.internal.org.jline.utils.Log;
import net.sf.jasperreports.engine.JRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Kelsas
 */
@RestController
@RequestMapping("/api")
@Api
public class PatientController {

    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonService personService;

    @Autowired
    private AllergiesService allergiesService;

    @Autowired
    private PatientIdentificationTypeService patientIdentificationTypeService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private PatientIdentifierService patientIdentifierService;

    @Autowired
    AuditTrailService auditTrailService;

    @Autowired
    private SummaryPatientService summaryPatientService;

    @GetMapping("/summarypatient")
    public ResponseEntity<?> getPatientsSummary() {
//        SummaryPatient foundPatients = (SummaryPatient) summaryPatientService.fetchAllPa();
        int tots = summaryPatientService.findTotal();
        System.out.println("TOTAL: "+tots);
        System.out.println("MALE_UNDER_5: "+summaryPatientService.findMaleUnder5());
        System.out.println("MALE_ABOVE_5: "+summaryPatientService.findMaleAbove5());
        System.out.println("FEMALE_UNDER_5: "+summaryPatientService.findFemaleUnder5());
        System.out.println("FEMALE_ABOVE_5: "+summaryPatientService.findFemaleAbove5());

        SummaryPatient foundPatients = new SummaryPatient();
        Map<String, Integer> sumPatients = new HashMap<>();

        // Add the summs
        sumPatients.put("TOTAL", summaryPatientService.findTotal());
        sumPatients.put("MALE_UNDER_5", summaryPatientService.findMaleUnder5());
        sumPatients.put("MALE_ABOVE_5", summaryPatientService.findMaleAbove5());
        sumPatients.put("FEMALE_UNDER_5", summaryPatientService.findFemaleUnder5());
        sumPatients.put("FEMALE_ABOVE_5", summaryPatientService.findFemaleAbove5());


        return ResponseEntity.ok(sumPatients);

//        if (foundPatients == null) {
//        if (tots == null) {
//            System.out.println("TOTS: "+tots);
//
//            return ResponseEntity.notFound().build();
//        } else {
//            System.out.println("TOTS: "+tots);
//
//            return ResponseEntity.ok(tots);
//        }
    }




    @PostMapping("/patients")
    @PreAuthorize("hasAuthority('create_patients')")
    public @ResponseBody
    ResponseEntity<?> createPatient(@RequestPart PatientData patientData, @RequestPart(name = "file", required = false) MultipartFile file) {

        Patient patient = this.patientService.createPatient(patientData, file);

        PatientData savedpatientData = patientService.convertToPatientData(patient);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/patients/{id}")
                .buildAndExpand(patient.getPatientNumber()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Created  patient " + patient.getFullName());
        return ResponseEntity.created(location).body(ApiResponse.successMessage("Patient successfuly created", HttpStatus.CREATED, savedpatientData));
    }

    @DeleteMapping("/patients/{id}")
    @PreAuthorize("hasAuthority('delete_patients')")
    public @ResponseBody
    ResponseEntity<?> deletePatient(@PathVariable("id") final Long id) {
        Patient patient = this.patientService.fetchPatientByPersonId(id);
        patientService.deletePatient(id);
        auditTrailService.saveAuditTrail("Patient", "Deleted  patient " + patient.getFullName());
        return ResponseEntity.ok("Successfully deleted " + patient.getFullName());
    }

    @GetMapping("/patients/{id}")
    @PreAuthorize("hasAuthority('view_patients')")
    public @ResponseBody
    ResponseEntity<PatientData> findPatientByPatientNumber(@PathVariable("id") final String patientNumber) {
        final Optional<PatientData> patient = this.patientService.fetchPatientByPatientNumber(patientNumber);

        if (patient.isPresent()) {
            auditTrailService.saveAuditTrail("Patient", "Viewed patient " + patient.get().getFullName());
            return ResponseEntity.ok(patient.get());
        } else {
            throw APIException.notFound("Patient Number {0} not found.", patientNumber);
        }
    }

    @GetMapping("/patients")
//    @PreAuthorize("hasAuthority('view_patients')")
    public ResponseEntity<?> fetchAllPatients(
            //@RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "results", required = false) Integer size,
            @RequestParam(value = "term", required = false) final String term,
            @RequestParam(value = "dateRange", required = false) final String dateRange,
            @RequestParam(value = "searchCriterion", required = false) final Long searchCriterion,

            UriComponentsBuilder uriBuilder) {
        System.out.println("Size " + size);
        System.out.println("Page " + page);
        Pageable pageable = null;
        pageable = PaginationUtil.createPage(page, size, Sort.by("id").descending());

        Page<PatientData> pageResult = null;
        if (searchCriterion == null) {
            pageResult = patientService.fetchAllPatients(term, dateRange, pageable).map(p -> patientService.convertToPatientData(p));
        } else {
            //find all identifiers
            PatientIdentificationType pit = patientIdentificationTypeService.fetchIdType(searchCriterion);
            Page<PatientIdentifier> piList = patientIdentifierService.fetchPatientIdentifiers(pit, term, pageable);
            List<PatientData> pdList = new ArrayList<>();
            for (PatientIdentifier pi : piList.getContent()) {
                PatientData pd = patientService.convertToPatientData(pi.getPatient());
                pdList.add(pd);
            }
            pageResult = new PageImpl<>(pdList, pageable, piList.getContent().size());
        }

        Pager<List<PatientData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(pageResult.getContent());
        PageDetails details = new PageDetails();
        details.setPage(pageResult.getNumber());
        details.setPerPage(pageResult.getSize());
        details.setTotalElements(pageResult.getTotalElements());
        details.setTotalPage(pageResult.getTotalPages());
        details.setReportName("Patient Register");
        details.setMale_under_5(summaryPatientService.findMaleUnder5());
        details.setMale_above_5(summaryPatientService.findMaleAbove5());
        details.setFemale_above_5(summaryPatientService.findFemaleAbove5());
        details.setFemale_under_5(summaryPatientService.findFemaleUnder5());
        //for Pato
//        details.setMale_under_5(pageResult.getSize()); //try a method for calculating male<5

        //
//         [
//        {
//            "id":70,
//                "selection":"25"
//        },
//        {
//            "id":71,
//                "selection":"50"
//        },
//        {
//            "id":72,
//                "selection":"50"
//        }
// ]
//        JsonArray content = new JsonArray(); // This is your parsed json object maybe shold happen in service
//        HashMap<String, Integer> count = new HashMap<>();
//        for (JsonElement element : content) {
//            JsonObject jsonObject = element.getAsJsonObject();
//            if(jsonObject.has("gender")) {
//                String selValue = jsonObject.get("gender").getAsString();
//                if(count.containsKey(selValue)) {
//                    count.put(selValue, count.get(selValue) + 1);
//                } else {
//                    count.put(selValue, 1);
//                }
//            }
//        }
//
//        count.get("M"); // returns all Male
//        System.out.println("tots "+count.get('M'));
//        count.get("F"); // returns all Female

//        expect().body("final_list.findall.size()",equalTo(2)).when().get("/list.json);
//        details.setMale_under_5(count.get("M")); //try a method for calculating male<5

        //details.setMale_under_5(patientService.fetchAllPatients().cou); //try a method for calculating male<5

        //
        pagers.setPageDetails(details);
        auditTrailService.saveAuditTrail("Patient", "Viewed all registered  patients ");
        return ResponseEntity.status(HttpStatus.OK)
                .body(pagers);
    }

    @PutMapping("/patients/{patientNumber}")
    @PreAuthorize("hasAuthority('edit_patients')")
    public @ResponseBody
    ResponseEntity<PatientData> updatePatient(@PathVariable("patientNumber") final String patientNumber,
                                              @RequestBody final PatientData patientData) {
        final Patient patient;
        if (this.patientService.patientExists(patientNumber)) {
            patient = patientService.findPatientOrThrow(patientNumber);

            //patient.setIsAlive(patientData.getIsAlive()==null?false: true);
            patient.setAllergyStatus(patientData.getAllergyStatus());
            patient.setBloodType(patientData.getBloodType());
            patient.setGender(patientData.getGender());
            patient.setGivenName(patientData.getGivenName());
            patient.setMaritalStatus(patientData.getMaritalStatus() != null ? patientData.getMaritalStatus().name() : null);
            patient.setMiddleName(patientData.getMiddleName());
            //patient.setPatientNumber(patientData.getPatientNumber());
            patient.setStatus(patientData.getStatus());
            patient.setSurname(patientData.getSurname());
            patient.setTitle(patientData.getTitle());
            patient.setDateOfBirth(patientData.getDateOfBirth());
            patient.setPrimaryContact(patientData.getPrimaryContact());
            patient.setResidence(patientData.getResidence());

            this.patientService.updatePatient(patientNumber, patient);
            auditTrailService.saveAuditTrail("Patient", "Edited  details for patient " + patient.getFullName());
        } else {
            //entDa
            throw APIException.notFound("Patient {0} not found.", patientNumber);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/patients/{id}")
                .buildAndExpand(patient.getPatientNumber()).toUri();

        return ResponseEntity.created(location).body(patientService.convertToPatientData(patient));
    }

    @GetMapping("/identifier/{identifier}/patient/{no}")
    @PreAuthorize("hasAuthority('view_patients')")
    public @ResponseBody
    ResponseEntity<PatientData> getPatientByIdentifier(@PathVariable("identifier") /*Identifier type*/ final String patientNumber, @PathVariable("no") final String patientNo) {
        final Optional<PatientData> patient = this.patientService.fetchPatientByPatientNumber(patientNumber);
        if (patient.isPresent()) {
            auditTrailService.saveAuditTrail("Patient", "Viewed  patient details for " + patient.get().getFullName());
            return ResponseEntity.ok(patient.get());
        } else {
            throw APIException.notFound("Patient Number {0} not found.", patientNumber);
        }
    }

    @PutMapping("/patients/{patientid}/contacts/{contactid}")
    @PreAuthorize("hasAuthority('edit_patients')")
    @ApiOperation(value = "Update a patient's contact details", response = PatientData.class)
    public @ResponseBody
    ResponseEntity<PatientData> updatePatientContacts(
            @PathVariable("patientid") final String patientNumber,
            @PathVariable("contactid") final Long contactid,
            @RequestBody final ContactData contactData) {
        final PersonContact contact;
        Patient patient = patientService.findPatientOrThrow(patientNumber);
        if (this.personService.contactExists(contactid)) {
            contact = personService.fetchContactById(contactid);
            contact.setEmail(contactData.getEmail());
            contact.setMobile(contactData.getMobile());
            contact.setTelephone(contactData.getTelephone());
            this.personService.updatePersonContact(contact);
        } else {
            throw APIException.notFound("Contact {0} not found.", contactid);
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/patients/{id}")
                .buildAndExpand(patient.getPatientNumber()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Edited  patient contact details for " + patient.getFullName());
        return ResponseEntity.created(location).body(patientService.convertToPatientData(patient));
    }

    @PutMapping("/patients/{patientid}/address/{addressid}")
    @PreAuthorize("hasAuthority('edit_patients')")
    @ApiOperation(value = "Update a patient's address details", response = PatientData.class)
    public @ResponseBody
    ResponseEntity<PatientData> updatePatientAddress(
            @PathVariable("patientid") final String patientNumber,
            @PathVariable("addressid") final Long addressid,
            @RequestBody final AddressDatas addressData) {
        final PersonAddress address;
        Patient patient = patientService.findPatientOrThrow(patientNumber);
        if (this.personService.addressExists(addressid)) {
            address = personService.fetchAddressById(addressid);
            address.setCountry(addressData.getCountry());
            address.setCounty(addressData.getCounty());
            address.setLine1(addressData.getLine1());
            address.setLine2(addressData.getLine2());
            address.setPostalCode(addressData.getPostalCode());
            address.setTown(addressData.getTown());
            this.personService.updatePersonAddress(address);
        } else {
            throw APIException.notFound("Address {0} not found.", addressid);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/patients/{id}")
                .buildAndExpand(patient.getPatientNumber()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Edited  patient Address details for " + patient.getFullName());
        return ResponseEntity.created(location).body(patientService.convertToPatientData(patient));
    }

    @PostMapping("/patients/{patientNumber}/next-of-kin")
    @PreAuthorize("hasAuthority('edit_patients')")
    public ResponseEntity<?> createPatientNextOfKin(
            @Valid @RequestBody PersonNextOfKinData data,
            @PathVariable("patientNumber") final String patientNumber) {
        Patient patient = patientService.findPatientOrThrow(patientNumber);
        PersonNextOfKin nextOfKin = new PersonNextOfKin();
        nextOfKin.setName(data.getName());
        nextOfKin.setPerson(patient);
        nextOfKin.setPrimaryContact(data.getPrimaryContact());
        nextOfKin.setRelationship(data.getRelationship());
        nextOfKin.setSpecialNote(data.getSpecialNote());
        nextOfKin = patientService.createNextOfKin(nextOfKin);
        Pager<PersonNextOfKinData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Next of kin created");
        pagers.setContent(PersonNextOfKinData.map(nextOfKin));
        auditTrailService.saveAuditTrail("Patient", "Created next of kin for patient " + patient.getFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @PutMapping("/patients/{nextOfKinId}/next-of-kin")
    @PreAuthorize("hasAuthority('edit_patients')")
    public ResponseEntity<?> editPatientNextOfKin(
            @Valid @RequestBody PersonNextOfKinData data,
            @PathVariable("nextOfKinId") final Long nextOfKinId) {
        PersonNextOfKin nextOfKin = patientService.findOrThrowNextOfKinById(nextOfKinId);
        nextOfKin.setName(data.getName());
        nextOfKin.setPrimaryContact(data.getPrimaryContact());
        nextOfKin.setRelationship(data.getRelationship());
        nextOfKin.setSpecialNote(data.getSpecialNote());
        nextOfKin = patientService.createNextOfKin(nextOfKin);
        Pager<PersonNextOfKinData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Next of kin updated");
        pagers.setContent(PersonNextOfKinData.map(nextOfKin));

        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @PostMapping("/patients/{id}/image")
    @PreAuthorize("hasAuthority('create_patients')")
    @ApiOperation(value = "Update a patient's image details", response = Portrait.class)
    public @ResponseBody
    ResponseEntity<PortraitData> postPatientImage(@PathVariable("id") final String patientNumber, @RequestParam final MultipartFile image) {
        Patient patient = patientService.findPatientOrThrow(patientNumber);

        try {

            Portrait portrait = this.patientService.createPortrait(patient, image);
            URI location = fromCurrentRequest().buildAndExpand(portrait.getId()).toUri();
            PortraitData data = modelMapper.map(portrait, PortraitData.class);
            auditTrailService.saveAuditTrail("Patient", "Uploaded  patient portrait for " + patient.getFullName());
            return ResponseEntity.created(location).body(data);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw APIException.internalError("Error saving patient's image ", ex.getMessage());
        }

    }

    @PostMapping("/patient_identification_type")
    @PreAuthorize("hasAuthority('create_patients')")
    public @ResponseBody
    ResponseEntity<?> createPatientIdtype(@RequestBody @Valid final PatientIdentificationType patientIdentificationType) {

        PatientIdentificationType patientIdtype = this.patientIdentificationTypeService.creatIdentificationType(patientIdentificationType);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/patient_identification_type/{id}")
                .buildAndExpand(patientIdtype.getId()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Created  patient identication type " + patientIdtype.getIdentificationName());
        return ResponseEntity.created(location).body(ApiResponse.successMessage("Identity type was successfully created", HttpStatus.CREATED, patientIdtype));
    }

    /*
    @GetMapping("/patient_identification_type")
    public ResponseEntity<List<PatientIdentificationType>> fetchAllPatientIdTypes(@RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {

//        return new ResponseEntity<List<PatientIdentificationType>>(patientIdentificationTypeService.fetchAllPatientIdTypes(), HttpStatus.OK);
        return new ResponseEntity<>(patientIdentificationTypeService.fetchAllPatientIdTypes(), HttpStatus.OK);
    }
     */
    @GetMapping("/patient_identification_type")
    @PreAuthorize("hasAuthority('view_patients')")
    public ResponseEntity<List<PatientIdentificationType>> fetchAllPatientIdTypes(@RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {

        auditTrailService.saveAuditTrail("Patient", "Viewed all patient identications type ");
        return new ResponseEntity<>(patientIdentificationTypeService.fetchAllPatientIdTypes(), HttpStatus.OK);
    }

    @GetMapping("/patient_identification_type/{id}")
    @PreAuthorize("hasAuthority('view_patients')")
    public PatientIdentificationType fetchAllPatientIdTypes(@PathVariable("id") final String patientIdType) {
        auditTrailService.saveAuditTrail("Patient", "Viewed  patient identication type identified by " + patientIdType);
        return patientIdentificationTypeService.fetchIdType(Long.valueOf(patientIdType));
    }

    /* Functions pertaining patient allergies */
    @PostMapping("/allergy")
    @PreAuthorize("hasAuthority('create_patients')")
    public @ResponseBody
    ResponseEntity<?> createPatientAllergy(@RequestBody @Valid final PatientAllergiesData patientAllergiesData) {
        Patient patient = patientService.findPatientOrThrow(patientAllergiesData.getPatientNumber());
        Allergy toSave = allergiesService.convertAllergyDataToEntity(patientAllergiesData);
        AllergyType allergyType = allergiesService.findAllergyTypeByCode(patientAllergiesData.getAllergyType());
        toSave.setPatient(patient);
        toSave.setAllergyType(allergyType);
        Allergy allergy = allergiesService.createPatientAllergy(toSave);
        PatientAllergiesData savedData = allergiesService.convertPatientAllergiesToData(allergy);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/allergy/{id}")
                .buildAndExpand(allergy.getId()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Created patient allergy for " + patient.getFullName());
        return ResponseEntity.created(location).body(savedData);
    }

    @PostMapping("/allergy-type")
    @PreAuthorize("hasAuthority('create_patients')")
    public @ResponseBody
    ResponseEntity<?> createAllergyType(@RequestBody @Valid final AllergyTypeData allergyTypeData) {
        AllergyType allergyType = allergiesService.createAllergyType(allergiesService.convertAllergyTypeDataToEntity(allergyTypeData));
        AllergyTypeData savedData = allergiesService.convertAllergyTypEntityToData(allergyType);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/allergy-type/{id}")
                .buildAndExpand(allergyType.getId()).toUri();
        auditTrailService.saveAuditTrail("Patient", "Created patient allergy type " + allergyType.getName());
        return ResponseEntity.created(location).body(savedData);
    }

    @GetMapping("/patient/{patientNumber}/allergy")
    @PreAuthorize("hasAuthority('view_patients')")
    public ResponseEntity<?> fetchAllPatientsAllergy(@PathVariable("patientNumber") final String patientNumber, @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) {
        Patient patient = patientService.findPatientOrThrow(patientNumber);
        auditTrailService.saveAuditTrail("Patient", "Viewed all patient allergies for " + patient.getFullName());
        Page<PatientAllergiesData> page = allergiesService.fetchPatientAllergies(patient, pageable).map(p -> allergiesService.convertPatientAllergiesToData(p));
        return new ResponseEntity<>(page.getContent(), HttpStatus.OK);
    }

    @GetMapping("/allergy-type")
    @PreAuthorize("hasAuthority('view_patients')")
    public ResponseEntity<List<AllergyTypeData>> fetchAllAllergyTypes() {
        List<AllergyTypeData> data = new ArrayList<>();
        allergiesService.findAllAllergyTypes().stream().map((at) -> {
            AllergyTypeData atd = new AllergyTypeData();
            atd.setCode(at.getCode());
            atd.setName(at.getName());
            return atd;
        }).forEachOrdered((atd) -> {
            data.add(atd);
        });
        auditTrailService.saveAuditTrail("Patient", "Viewed all patient allergy types ");
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    //PDF Reports
//    @RequestMapping(value = "/patient/export-patient-data", method = RequestMethod.GET)
//    public void export(ModelAndView model, HttpServletResponse response) throws IOException, JRException, SQLException {
//        JasperPrint jasperPrint = null;
//
//        response.setContentType("application/x-download");
//        response.setHeader("Content-Disposition", String.format("attachment; filename=\"patient.pdf\""));
//
//        OutputStream out = response.getOutputStream();
//        jasperPrint = patientService.exportPatientPdfFile();
//        JasperExportManager.exportReportToPdfStream(jasperPrint, out);
//    }
    //PDF Reports
    @RequestMapping(value = "/patient/patientFile", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('view_patients')")
    public void exportPatientFile(HttpServletResponse response) throws JRException, SQLException, IOException {
        String contentType = null;
        patientService.exportPatientPdfFile(response);
    }

    /* Functions pertaining patient identifier*/
    @PostMapping("/patients/{patientNumber}/identifiers")
    @PreAuthorize("hasAuthority('edit_patients')")
    public ResponseEntity<?> addPatientIdentifier(
            @Valid @RequestBody PersonIdentifierData data,
            @PathVariable("patientNumber") final String patientNumber) {
        Patient patient = patientService.findPatientOrThrow(patientNumber);

        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(patientIdentificationTypeService.fetchIdType(data.getIdType()));
        patientIdentifier.setValue(data.getIdentificationValue());
        patientIdentifier.setPatient(patient);

        PatientIdentifier savedPi = patientIdentifierService.createPatientIdentifier(patientIdentifier);

        Pager<PersonIdentifierData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Patient identification type created");
        pagers.setContent(patientIdentifierService.convertIdentifierEntityToData(savedPi));

        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @PutMapping("/patients/{patientNumber}/identifiers/{identifierId}")
    @PreAuthorize("hasAuthority('edit_patients')")
    public ResponseEntity<?> editPatientIdentifier(
            @Valid @RequestBody PersonIdentifierData data,
            @PathVariable("patientNumber") final String patientNumber,
            @PathVariable("identifierId") final Long identifierId
    ) {
        Patient patient = patientService.findPatientOrThrow(patientNumber);
        Optional<PatientIdentifier> pi = patientIdentifierService.fetchPatientIdentifierByPatientAndId(patient, identifierId);
        if (!pi.isPresent()) {
            throw APIException.notFound("Patient identifier notified ", "");
        }
        PatientIdentifier patientIdentifier = pi.get();
        patientIdentifier.setType(patientIdentificationTypeService.fetchIdType(data.getIdType()));
        patientIdentifier.setValue(data.getIdentificationValue());

        PatientIdentifier savedPi = patientIdentifierService.createPatientIdentifier(patientIdentifier);

        Pager<PersonIdentifierData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Patient identification type updated");
        pagers.setContent(patientIdentifierService.convertIdentifierEntityToData(savedPi));

        return ResponseEntity.status(HttpStatus.OK).body(pagers);
    }

    @GetMapping("/patients/tafuta")
    public @ResponseBody
    ResponseEntity<List<PatientData>> tafutaPatient(@RequestParam("q") final String term) {
        List<PatientData> patient = this.patientService.tafuta(term).stream()
                .map(p -> patientService.convertToPatientData(p))
                .collect(Collectors.toList());

        return ResponseEntity.ok(patient);
    }

}
