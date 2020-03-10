package io.smarthealth.clinical.laboratory.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class LabTestData {

    private Long id;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String shortName;
    private String testName;
    private Long categoryId;
    private String category;
    private String gender;
    private Boolean requiresConsent;
    private Boolean hasReferenceValue;
    private String turnAroundTime;
    private Boolean active;
    private List<AnalyteData> analytes = new ArrayList<>();

}