package io.smarthealth.clinical.lab.domain;

import io.smarthealth.infrastructure.domain.Identifiable;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Entity
@Data
//@NamedQuery(name="tets",query = "SELECT e FROM Analyte e WHERE e.testType = :testType AND e.sex = :gender AND :age BETWEEN e.startAge and e.endAge ")
@Table(name = "lab_discipline")
public class Discipline extends Identifiable {
    private String discipline;
}
