/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.organization.person.patient.domain;

import io.smarthealth.infrastructure.domain.Auditable;
import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Data;

/**
 *
 * @author Simon.Waweru
 */
@Data
@Entity
public class PatientCondition extends Auditable {
    @ManyToOne
    @JoinColumn(name = "patient_id", foreignKey = @ForeignKey(name = "fk_patient_condition_patient_id"))
    private Patient patient;
    private String code;
    private String name;
}
