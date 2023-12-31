/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.record.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smarthealth.clinical.record.domain.DoctorRequest;
import io.smarthealth.infrastructure.lang.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class OrdersRequest {

    private Long id;
    private String patientName;
    private String patientNumber;
    private String item;
    private Long itemId;
    private String itemCode;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDateTime orderDate;
    private String orderNumber;
    private String notes;
    private String requestedBy;
    private Long requestedById;
    private DoctorRequestData.RequestType requestType;
    private String urgency;
    private String visitNumber;
    private BigDecimal serviceRate;

    public static OrdersRequest of(DoctorRequest request) {
        OrdersRequest order = new OrdersRequest();
        order.setId(request.getId());
        if (request.getPatient() != null) {
            order.setPatientName(request.getPatient().getFullName());
            order.setPatientNumber(request.getPatient().getPatientNumber());
        }
        if (request.getItem() != null) {
            order.setItem(request.getItem().getItemName());
            order.setItemId(request.getItem().getId());
            order.setItemCode(request.getItem().getItemCode());
            order.setServiceRate(request.getItem().getRate());
        }
        order.setOrderDate(request.getOrderDate());
        order.setOrderNumber(request.getOrderNumber());
        order.setNotes(request.getNotes());
        if(request.getRequestedBy()!=null){
        order.setRequestedBy(request.getRequestedBy().getName());
        order.setRequestedById(request.getRequestedBy().getId());
        }
        order.setRequestType(request.getRequestType());
        if(request.getVisit()!=null) {
            order.setVisitNumber(request.getVisit().getVisitNumber());
        }
//        if(request.getUrgency()!=null){
//        order.setUrgency(request.getUrgency());
//        }
        return order;
    }
}
