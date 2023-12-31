/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.pharmacy.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.clinical.pharmacy.domain.enumeration.TransactionType;
import io.smarthealth.infrastructure.lang.Constants;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.*;

import lombok.Data;

/**
 * @author Kelsas
 */
@Data
public class DispensedDrugData implements Serializable {
    // What was I thinking this could have just extended bill items
    private Long id;
    private Long storeId;
    private String storeName;

    private String patientNumber;
    private String patientName;

    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate dispensedDate;


    private Long drugId;
    private String drug;
    private String prescriptionNo;
    private String billNumber;
    private Double qtyIssued;
    private Double price;
    private Double cost;
    private Double amount;
    private String units;
    private String instructions;
    private String doctorName;
    private Boolean paid;
    private Boolean collected;
    private String dispensedBy;
    private String collectedBy;
    private Boolean isReturn = false;
    private String returnReason;
    private LocalDate returnDate;
    private Double returnedQuantity;
    private Double qtyBalance;
    private String otherReference;
    private Boolean walkinFlag = Boolean.FALSE;
    private String batchNumber;
    private Boolean voided;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private Long patientBillItemId;
    private String transactionId;
}
