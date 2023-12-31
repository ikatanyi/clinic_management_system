package io.smarthealth.clinical.record.domain;

import io.smarthealth.clinical.record.data.enums.EncounterFormType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;

/**
 * Patient Consultation Doctor's patient notes
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "patient_clinical_notes")
public class PatientNotes extends ClinicalRecord {

    @Lob
    private String chiefComplaint;
    @Lob
    private String historyNotes; //history of present complaints
    @Lob
    private String examinationNotes;
    @Lob
    private String socialHistory;
    @Lob
    private String briefNotes;

    @Enumerated(EnumType.STRING)
    private EncounterFormType encounterForm;
}
