package io.smarthealth.accounting.doctors.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.accounting.doctors.domain.DoctorInvoice.TransactionType;
import io.smarthealth.infrastructure.lang.Constants;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data 
public class DoctorInvoiceData  {

    private Long id;
    private Long doctorId;
    private String doctorName;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate invoiceDate;
      private String invoiceNumber;
      
    private String patientNumber;
    private String patientName; 
    
     private Long serviceId;
    private String serviceName;
    private String serviceCode;
      
    private Boolean paid;
    private BigDecimal amount;
    private BigDecimal balance;
    private String paymentMode;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private String transactionId;
}