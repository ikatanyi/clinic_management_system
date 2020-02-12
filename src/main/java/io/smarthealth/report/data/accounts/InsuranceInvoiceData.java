/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.report.data.accounts;

import java.time.LocalDate;
import java.util.Date;
import lombok.Data;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
public class InsuranceInvoiceData {
    private Double amount;
    private Double balance;
    private Double discount;
    private Double paid;
    private String patientId;
    private String patientName;
    private String payer;
    private String payee;
    private String invoiceNo;   
    private String dueDate;
    private String date;
    private String status;
    private Double lab;
    private Double pharmacy;
    private Double radiology;
    private Double consultation;
    private Double procedure;
    private Double other;
}