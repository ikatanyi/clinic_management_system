package io.smarthealth.clinical.laboratory.domain;

import io.smarthealth.clinical.laboratory.data.LabRegisterTestData;
import io.smarthealth.clinical.laboratory.domain.enumeration.LabTestStatus;
import io.smarthealth.infrastructure.domain.Identifiable;
import io.smarthealth.stock.item.domain.Item;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringExclude;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "lab_register_tests")
public class LabRegisterTest extends Identifiable {

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_lab_register_tests_request_id"))
    private LabRegister labRegister;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_lab_register_tests_test_id"))
    private LabTest labTest;
    private BigDecimal price;
    private Long requestId; //reference to doctor's request order number

    private String specimen;
    private Boolean collected;
    private String collectedBy;
    private LocalDateTime collectionDateTime;

    private Boolean entered; //results entered
    private String enteredBy;
    private LocalDateTime entryDateTime;

    private Boolean validated;
    private String validatedBy;
    private LocalDateTime validationDateTime;

    private String referenceNo; //payment reference
    private Boolean paid; //  test paid

    @Enumerated(EnumType.STRING)
    private LabTestStatus status; // overal test status

    private Boolean voided = Boolean.FALSE;
    private String voidedBy;
    private LocalDateTime voidDatetime;
    private Boolean isPanel;
    private Boolean resultRead = Boolean.FALSE;
    @ManyToOne
    @ToStringExclude
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_lab_register_parent_test_id"))
    private LabTest parentLabTest;

    @OneToMany(mappedBy = "labRegisterTest")
    private List<LabResult> labResults;

    //we can have attachment link here
    private String attachment;

    private String testName;

    public LabRegisterTestData toData(Boolean expand) {
        LabRegisterTestData data = new LabRegisterTestData();
        data.setId(this.getId());

        data.setCollected(this.collected);
        data.setCollectedBy(this.collectedBy);
        data.setCollectionDateTime(this.collectionDateTime);

        data.setEntered(this.entered);
        data.setEnteredBy(this.enteredBy);
        data.setEntryDateTime(this.entryDateTime);

        data.setValidated(this.validated);
        data.setValidatedBy(this.validatedBy);
        data.setValidationDateTime(this.validationDateTime);

        data.setTestPrice(this.price);
        if (this.labRegister != null) {
            data.setLabRegisterId(this.labRegister.getId());
            data.setOrderNumber(this.labRegister.getOrderNumber());
            data.setRequestedBy(this.labRegister.getRequestedBy());
            data.setLabNumber(this.labRegister.getLabNumber());
            data.setPatientNumber(this.labRegister.getPatientNo());

            if (this.labRegister.getVisit() != null) {
                data.setPatientName(this.labRegister.getVisit().getPatient().getGivenName());
                data.setDOB(this.labRegister.getVisit().getPatient().getDateOfBirth());
                data.setGender(this.labRegister.getVisit().getPatient().getGender());
            }
        }

        data.setPaid(this.paid);
        data.setReferenceNo(this.referenceNo);
        data.setRequestId(this.requestId);

        data.setSpecimen(this.specimen);
        data.setStatus(this.status);
        data.setIsPanel(this.isPanel);

        if (this.labTest != null) {
            data.setTestId(this.labTest.getId());
            data.setTestCode(this.labTest.getCode());
            data.setTestName(this.labTest.getTestName());

            if (this.labTest.getDispline() != null) {
                data.setDiscipline(this.labTest.getDispline().getDisplineName());
            }

            data.setWithRef(this.labTest.getHasReferenceValue() != null ? this.labTest.getHasReferenceValue() : true);
        }

        if (this.labTest == null) {
            data.setTestCode(this.testName);
            data.setTestName(this.testName);
        }

        if (this.parentLabTest != null) {
            data.setParentTest(this.parentLabTest.getTestName());
        }

        data.setAttachment(this.attachment);
        //include the results 
        if (expand) {
            data.setLabResults(
                    this.getLabResults()
                            .stream()
                            .map(x -> x.toData())
                            .collect(Collectors.toList())
            );
        }

        return data;

    }

//    private String getAttachmentUrl() {
//        if (this.attachment != null) {
//
//            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
//                    .path("/api/downloadFile/")
//                    .path("Laboratory/")
//                    .path(this.attachment)
//                    .toUriString();
//            
//            return fileDownloadUri;
//        }
//        return null;
//    }
}
