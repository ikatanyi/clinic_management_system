package io.smarthealth.accounting.payment.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.accounting.cashier.data.ShiftData;
import io.smarthealth.accounting.payment.domain.enumeration.ReceiptAndPaymentMethod;
import io.smarthealth.clinical.visit.data.enums.VisitEnum;
import io.smarthealth.infrastructure.lang.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.smarthealth.accounting.payment.domain.enumeration.ReceiptType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author Kelsas
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptData {
    private Long id;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDateTime transactionDate;
    private String payer;
    private String description; //Insurance payment | Cheque deposit
    private BigDecimal amount;
    private BigDecimal paid;
    private BigDecimal tenderedAmount;
    private BigDecimal refundedAmount;

    @Enumerated(EnumType.STRING)
    private ReceiptAndPaymentMethod paymentMethod;
    private String receiptNo;
    private String referenceNumber; //voucher no,
    private String transactionType;
    private String transactionNo;
    private String shiftNo;
    private String currency;
    private String createdBy;
    private Boolean prepayment;
    private String receivedFrom;
    private Boolean voided;
    private String voidedBy;
    private String comments;
    private ReceiptType receiptType;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime voidedDatetime;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime createdOn;
    private List<ReceiptTransactionData> transactions = new ArrayList<>();
    private List<ReceiptItemData> receiptItems = new ArrayList<>();
    private ShiftData shiftData;
    private VisitEnum.VisitType visitType;

}
