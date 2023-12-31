package io.smarthealth.clinical.admission.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.clinical.admission.domain.Admission;
import io.smarthealth.clinical.visit.data.PaymentDetailsData;
import io.smarthealth.clinical.visit.data.enums.VisitEnum;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;
import io.smarthealth.infrastructure.lang.Constants;
import io.smarthealth.organization.person.domain.enumeration.Gender;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.smarthealth.infrastructure.lang.Constants.DATE_TIME_PATTERN;

/**
 * @author Kelsas
 */
@Data
public class AdmissionData {

    private Long id;
    @ApiModelProperty(hidden = true)
    private String admissionNumber; //this should be same as visit number
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_PATTERN)
    private LocalDateTime admissionDate;
    private String patientNumber;
    @ApiModelProperty(hidden = true)
    private String patientName;
    @ApiModelProperty(hidden = true)
    private Integer age;
    @ApiModelProperty(hidden = true)
    private Gender gender;

    private String formattedGender;

    private String admittingReason;

    private PaymentMethod paymentMethod;

    private Long wardId;

    @ApiModelProperty(hidden = true)
    private String wardName;

    private Long roomId;

    @ApiModelProperty(hidden = true)
    private String roomName;

    private Long bedId;

    @ApiModelProperty(hidden = true)
    private String bedName;

    private Long bedTypeId;
    @ApiModelProperty(hidden = true)
    private BedTypeData bedTypeData;
    @ApiModelProperty(hidden = true)
    private Boolean discharged = Boolean.FALSE;
    @ApiModelProperty(hidden = true)
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime dischargeDate;

    @ApiModelProperty(hidden = true)
    private String admittingDoctor;

    @ApiModelProperty(hidden = true)
    private String dischargedBy;
    @ApiModelProperty(example = "CheckIn,CheckOut,Admitted,Transferred, Discharged, Booked")
    private VisitEnum.Status status;

    private String narration;

    private PaymentDetailsData paymentDetailsData;

    private String inpatientNumber;
    private Integer stayDuration;
    private String formattedAge;
    private String dischargeNo;

    private String payerName;
    private String schemeName;

    private List<CareTeamData> careTeam = new ArrayList<>();
    private List<EmergencyContactData> emergencyContactData = new ArrayList();

    //OP to IP conversion parameters
    @ApiModelProperty(hidden = false, notes = "Required Only when OP is converted to IP")
    private String opVisitNumber;

    @ApiModelProperty(hidden = false, notes = "Required Only when OP is converted to IP")
    private Long admissionRequestId;

    public static AdmissionData map(Admission adm) {
        AdmissionData d = new AdmissionData();

        d.setId(adm.getId());

        d.setAdmissionDate(adm.getAdmissionDate());
        d.setAdmissionNumber(adm.getAdmissionNo());
        d.setBedId(adm.getBed().getId());
        d.setBedName(adm.getBed().getName());
        d.setBedTypeData(adm.getBedType().toData());
        d.setBedTypeId(adm.getBedType().getId());
        d.setCareTeam(adm.getCareTeam().stream().map(c -> CareTeamData.map(c)).collect(Collectors.toList()));
        d.setEmergencyContactData(adm.getEmergencyContacts().stream().map(c -> c.toData()).collect(Collectors.toList()));
        d.setDischargeDate(adm.getDischargeDate());
        d.setDischarged(adm.getDischarged());
        d.setDischargedBy(adm.getDischargedBy());
        d.setDischargeNo(adm.getDischargeNo());


        d.setAdmittingReason(adm.getAdmissionReason());
        if (adm.getHealthProvider() != null)
            d.setAdmittingDoctor(adm.getHealthProvider().getFullName());
        d.setPatientName(adm.getPatient().getFullName());
        d.setPatientNumber(adm.getPatient().getPatientNumber());
        d.setAge(adm.getPatient().getAge());
        d.setGender(adm.getPatient().getGender());
        d.setPaymentMethod(adm.getPaymentMethod());
        if (adm.getRoom() != null) {
            d.setRoomId(adm.getRoom().getId());
            d.setRoomName(adm.getRoom().getName());
        }
        d.setStatus(VisitEnum.Status.Admitted);
        d.setWardId(adm.getWard().getId());
        d.setWardName(adm.getWard().getName());


        LocalDateTime end = adm.getDischargeDate() != null ? adm.getDischargeDate() : LocalDateTime.now();
        int duration = 0;
        if (adm.getAdmissionDate().toLocalDate().equals(adm.getDischargeDate())) {
            duration = 1;
        } else {
            Period period = Period.between(adm.getAdmissionDate().toLocalDate(), end.toLocalDate());
            duration = period.getDays();
        }
        d.setStayDuration(duration);

        if (adm.getPatient().getGender() != null) {
            d.setFormattedGender(adm.getPatient().getGender().getFormattedValue());
        }
        d.setFormattedAge(adm.getPatient().getFormattedAge());

        return d;
    }

    public static Admission map(AdmissionData adm) {
        Admission d = new Admission();
        d.setAdmissionDate(adm.getAdmissionDate());
        // d.setCareTeam(adm.getCareTeam().stream().map(c -> CareTeamData.map(c)).collect(Collectors.toList()));
        d.setDischargeDate(adm.getAdmissionDate());
        d.setDischarged(adm.getDischarged());
        d.setDischargedBy(adm.getDischargedBy());
        d.setPaymentMethod(adm.getPaymentMethod());
        d.setStatus(adm.getStatus());
        return d;
    }

}
