package io.smarthealth.debtor.claim.dispatch.domain;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import io.smarthealth.accounting.invoice.domain.Invoice;
import io.smarthealth.debtor.payer.domain.Payer;
import io.smarthealth.infrastructure.domain.Auditable;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
@Entity
@Table(name = "patient_invoice_dispatch")
public class Dispatch extends Auditable {

    private String dispatchNo;
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk__dispatch_payer_id"))
    private Payer payer;
    
    private LocalDate dispatchDate;
    private String comments;
    
    @OneToMany
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_dispatch__patient_invoice_id"))
    private List<Invoice>dispatchedInvoice;    
}
