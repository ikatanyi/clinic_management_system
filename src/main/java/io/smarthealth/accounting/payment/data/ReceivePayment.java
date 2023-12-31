package io.smarthealth.accounting.payment.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.accounting.payment.domain.enumeration.ReceiptAndPaymentMethod;
import io.smarthealth.infrastructure.lang.Constants;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 *
 * @author Kelsas
 */
@Data
public class ReceivePayment {

    public enum Type {
        Patient,
        Insurance,
        Others
    }
 
    private String description;

    private Long payerId;
    private String payer;
    private String payerNumber;

    private Type type;
    private Boolean walkin;
    private BigDecimal tenderedAmount;
    private BigDecimal amount; //total amount paid
    private String currency;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime date;
    private String shiftNo;
    private String transactionNo;

    @Enumerated(EnumType.STRING)
    private ReceiptAndPaymentMethod paymentMethod; //this should be paymode i.e Cash/Credit not payment method -Simon's comments
    private String referenceNumber;
    private String visitNumber;
    private String patientNumber;
    private String receiptNo; 
    
    private List<BillReceiptedItem> billItems = new ArrayList<>();

    private List<ReceiptMethod> payment = new ArrayList<>();

    public String getDescription() {
        if (this.description != null) {
            return this.description;
        }

        switch (this.type) {
            case Patient:
                return "Patient Payment";
            case Insurance:
                return "Insurance Payment";
            default:
                return "Other Payment";
        }
    }
}
