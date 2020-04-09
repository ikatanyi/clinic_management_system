package io.smarthealth.accounting.payment.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.infrastructure.lang.Constants;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

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
    private Long payerId;
    private String payer;
    private String payerNumber;
    private Type type;
    private Boolean walkin;
    private BigDecimal amount; //total amount paid
    private String currency;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime date;
    private String shiftNo;
    private String transactionNo;
    private String paymentMethod;
    private String referenceNumber;
    //paying for what now
    //determine the invoice or bill being paid for
    private List<BilledItem> billItems = new ArrayList<>();

    private List<ReceiptMethod> payment = new ArrayList<>();

}