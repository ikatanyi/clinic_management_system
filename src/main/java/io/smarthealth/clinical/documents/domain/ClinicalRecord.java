package io.smarthealth.clinical.documents.domain;

import io.smarthealth.infrastructure.domain.Auditable;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.clinical.visit.domain.Visit;
import java.time.LocalDateTime;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import lombok.Data;

/**
 * Base for all Clinical Documentations of Patient Medical Records
 *
 * @author Kelsas
 */
@Data
@MappedSuperclass
public abstract class ClinicalRecord extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Patient patient;
    @ManyToOne
    private Visit visit;
    @ManyToOne
    private Employee healthcareProvider;
    private LocalDateTime dateRecorded;
    private boolean voided = false;
    private String voidedBy;
    private LocalDateTime voidedDate;

}
