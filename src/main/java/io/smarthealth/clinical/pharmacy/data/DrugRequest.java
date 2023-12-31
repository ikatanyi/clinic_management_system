package io.smarthealth.clinical.pharmacy.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.domain.enumeration.ExcessAmountPayMethod;
import io.smarthealth.infrastructure.lang.Constants;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import io.smarthealth.organization.person.domain.WalkIn;
import lombok.Data;

/**
 * @author Kelsas
 */
@Data
public class DrugRequest {

    private Long id;
    private Long storeId;
    private String storeName;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate dispenseDate;
    private String patientNumber;
    private String patientName;
    private String visitNumber;
    private String billNumber;
    private String transactionId; //Receipt n. or Invoice No
    private String paymentMode;
    private Double balance;
    private Double Amount; // this QTY*PRICE
    private Double taxes;
    private Double discount;
    private Boolean isWalkin;

    @JsonIgnore
    private WalkIn walkIn;

    @Enumerated(EnumType.STRING)
    private BillStatus status;

    private List<DrugItemRequest> drugItems = new ArrayList<>();

    /* Start Limit manager variable */
    private Boolean allowedToExceedLimit = Boolean.FALSE;
    @Enumerated(EnumType.STRING)
    private ExcessAmountPayMethod surpassedAmountPaymentMethod;
    /* End Limit manager variables */
}
