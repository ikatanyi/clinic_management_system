
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.organization.person.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.smarthealth.infrastructure.common.APIResponse;
import io.smarthealth.infrastructure.common.BiometricUtil;
import io.smarthealth.organization.person.data.BiometricDataIndicator;
import io.smarthealth.organization.person.data.PatientBiometricRequest;
import io.smarthealth.organization.person.domain.Biometrics;
import io.smarthealth.organization.person.domain.Person;
import io.smarthealth.organization.person.patient.data.PatientData;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.organization.person.service.BiometricService;
import java.util.UUID;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author kelvin.sasaka
 */
@Slf4j
@RestController
@RequestMapping("/api/biometrics")
public class BiometricController {

    @Autowired
    private BiometricService biometricService;

    @Autowired
    PatientService patientService;

    @PostMapping("/patient/enroll")
    public @ResponseBody
    ResponseEntity<?> enrollPatientFP(@Valid @RequestBody PatientBiometricRequest biometrics) {
        Patient patient = patientService.findPatientOrThrow(biometrics.getPatientNumber());

        biometrics.getBiometric().stream().map((bd) -> {
            Biometrics finger = new Biometrics();
            finger.setIndicator(BiometricDataIndicator.valueOf(bd.getIndicator()));
            finger.setPerson(patient);
            finger.setData(bd.getData());
            finger.setTemplate(BiometricUtil.toCacheFingerprintJson(bd.getData()));
            return finger;
        }).forEachOrdered((finger) -> {
            biometricService.addBiometric(finger);
        });
        return ResponseEntity.ok().body(APIResponse.successMessage("Fingerprint data has been successfully", HttpStatus.CREATED, ""));
    }

    @PostMapping("/patient/verify")
    public @ResponseBody
    ResponseEntity<?> verifyPatient(@RequestBody JsonNode biometrics) {
        String fingerprint = biometrics.get("finger_data").textValue();
        String success = biometricService.verifyPatient(fingerprint);

        if (success != null) {
            //fetch patient by person id
            Patient patient = patientService.fetchPatientByPersonId(Long.valueOf(success));
            PatientData patientData = patientService.convertToPatientData(patient);
            return ResponseEntity.ok().body(APIResponse.successMessage("Success", HttpStatus.OK, patientData));
        }
        return ResponseEntity.ok().body(APIResponse.errorMessage("Failed", HttpStatus.OK, "No  Fingerprint Match Found!"));
    }

}
