package io.smarthealth.clinical.admission.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.clinical.admission.domain.DischargeSummary;
import io.smarthealth.infrastructure.lang.Constants;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class DischargeData {

    private Long id;
    private String patientNumber;
    private String patientName;
    private String gender;
     private String doctor;
    private String admissionNumber;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime admissionDate;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime dischargeDate;
    private String dischargeNo;
    private String dischargeMethod;
    private String dischargedBy;
    private String diagnosis;
    private String instructions;
    private String outcome;

}
