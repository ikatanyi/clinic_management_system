/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.integration.metadata.PatientData;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
public class Diagnosis {
    @ApiModelProperty(hidden=true)
    @JsonProperty("Stage")
    private String Stage="P";    
    @ApiModelProperty(example="ICD10")
    @JsonProperty(value = "Code_Type")
    private String Code_Type="ICD10";    
    @ApiModelProperty(example="icd10_code")
    @JsonProperty("Code")
    private String code="0";
}
