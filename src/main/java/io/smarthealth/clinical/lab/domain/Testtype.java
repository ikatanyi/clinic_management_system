package io.smarthealth.clinical.lab.domain;

import io.smarthealth.infrastructure.domain.Identifiable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "test_type")
public class Testtype extends Identifiable {
    
    private Long id;
    private String serviceCode;
    private String testType; //government classifications
    private String disciplineCode;
    private String specimenCode;
    private String containerCode;
    private Boolean consent; 
    private Boolean withtRef; 
    private Boolean refOut; 
    private Long duration;
    private String durationDesc;
    private String notes;
    
    
    @OneToMany(mappedBy = "testType")
    private List<Analyte> analytes = new ArrayList<>();
}
