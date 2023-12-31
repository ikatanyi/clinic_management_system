/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.accounting.cashier.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.accounting.cashier.domain.*;
import io.smarthealth.infrastructure.lang.Constants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class ShiftData {
   private Long id;
    private String cashPoint;
    private String cashier;
    private Long cashierId;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime startDate;
    @JsonFormat(pattern = Constants.DATE_TIME_PATTERN)
    private LocalDateTime endDate;
    private String shiftNo;
    private ShiftStatus status;
    private Boolean reconcile;
    private LocalDate dateReconcile;
    private BigDecimal shiftAmount;
    private BigDecimal collectedAmount;
}
