package io.smarthealth.accounting.billing.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.smarthealth.accounting.accounts.domain.TransactionType;
import io.smarthealth.accounting.billing.data.BillItemData;
import io.smarthealth.accounting.billing.data.nue.BillItem;
import io.smarthealth.accounting.billing.domain.enumeration.BillEntryType;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.clinical.theatre.data.TheatreProvider;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;
import io.smarthealth.debtor.payer.domain.Scheme;
import io.smarthealth.infrastructure.domain.Auditable;
import io.smarthealth.stock.item.domain.Item;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.*;

import io.smarthealth.stock.item.domain.enumeration.ItemCategory;
import lombok.Data;

/**
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "patient_billing_item")
public class PatientBillItem extends Auditable {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_patient_bill_item_bill_id"))
    private PatientBill patientBill;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_patient_bill_item_item_id"))
    private Item item;

    private LocalDate billingDate;
    private String transactionId;
    private Double quantity;
    private Double price;
    private Double discount = 0.0;
    @Transient
    private Double subTotal = 0.0; // (qty*price)-discount
    private Double taxes = 0.0;
    private Double amount = 0.0; // subtotal + tax %
    private Double balance = 0.0;
    private String servicePoint;
    private Long servicePointId;
    private Boolean paid;
    private boolean finalized = false;

    @Enumerated(EnumType.STRING)
    private PaymentMethod billPayMode;//Cash/Insurance

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_patient_bill_item_scheme_id"))
    private Scheme scheme;

    @Enumerated(EnumType.STRING)
    private BillStatus status;

    @Enumerated(EnumType.STRING)// to represent a credit or a debit for easy balance man
    private BillEntryType entryType;

    @Transient
    private Long medicId;

    private Long requestReference;
    /**
     * Reference payment details i.e. Receipt or an Invoice used to settle this
     * bill
     */
    private String paymentReference;
    private String invoiceNumber;

//    @Enumerated(EnumType.STRING)
//    private TransactionType transactionType;

    @Transient
    List<TheatreProvider> theatreProviders;
    @Transient
    private Long storeId;

    public BillItemData toData() {
        BillItemData data = new BillItemData();
        data.setId(this.getId());
        if (this.getPatientBill() != null) {
            data.setBillNumber(this.patientBill.getBillNumber());
            data.setVisitNumber(this.patientBill.getVisit() != null ? this.patientBill.getVisit().getVisitNumber() : null);
            if (this.patientBill.getPatient() != null) {
                data.setPatientName(this.patientBill.getPatient().getFullName());
                data.setPatientNumber(this.patientBill.getPatient().getPatientNumber());

            }
        }
        data.setBillingDate(this.billingDate);
        data.setPrice(this.price);
        data.setQuantity(this.quantity);
        data.setAmount(this.amount);
        data.setTaxes(this.taxes);
        data.setBalance(this.balance);
        data.setDiscount(this.discount);
        data.setTransactionId(this.transactionId);
        data.setStatus(this.status);

        data.setCreatedBy(this.getCreatedBy());

        if (this.item != null) {
            data.setItemId(this.item.getId());
            data.setItemCode(this.item.getItemCode());
            if (this.item.getCategory() == ItemCategory.Receipt) {
                data.setItem("Receipt No. " + this.getPaymentReference());
            } else {
                data.setItem(this.item.getItemName());
            }
            data.setItemCategory(this.item.getCategory());
        }
        data.setServicePoint(this.servicePoint);
        data.setServicePointId(this.servicePointId);
        data.setPaid(this.paid);
        data.setRequestReference(this.requestReference);
        data.setPaymentReference(this.paymentReference);

        if (this.getPatientBill().getWalkinFlag() != null && this.getPatientBill().getWalkinFlag()) {
            data.setPatientName(this.getPatientBill().getOtherDetails());
            data.setPatientNumber(this.getPatientBill().getReference());
        }
        data.setWalkinFlag(this.getPatientBill().getWalkinFlag());
        data.setPaymentMethod(this.billPayMode);
        data.setFinalized(this.finalized);
        data.setInvoiceNumber(this.invoiceNumber);
        if (this.entryType != null) {
            data.setEntryType(this.entryType);
        }
        if (scheme != null) {
            data.setSchemeId(scheme.getId());
            data.setSchemeName(scheme.getSchemeName());
        }
        if (this.billPayMode != null && billPayMode == PaymentMethod.Insurance) {

            data.setPaymentStatus(finalized ? "Finalized" : "Draft");
        } else {
            data.setPaymentStatus(status.name());
        }
        return data;
    }

    public BillItem toBillItem() {
        BillItem data = new BillItem();
        data.setId(this.getId());
        data.setBillId(this.patientBill.getId());
        if (this.item != null) {
            data.setItemId(this.item.getId());
            data.setItemCode(this.item.getItemCode());
            data.setItem(this.item.getItemName());
            data.setItemCategory(this.item.getCategory());
        }
        data.setBillingDate(this.getBillingDate());
        data.setQuantity(this.quantity);
        data.setPrice(this.price);
        data.setAmount(this.amount);
        data.setDiscount(this.discount);
        data.setTax(this.taxes);

        data.setNetAmount(((toDouble(this.quantity) * toDouble(this.price)) - toDouble(discount)) + toDouble(taxes));

        data.setServicePoint(this.servicePoint != null ? this.servicePoint : "Other");
        data.setServicePointId(this.servicePointId);
        data.setPaid(this.paid);
        data.setTransactionId(this.transactionId);
        data.setReference(this.paymentReference);
        data.setStatus(this.status);
        data.setBillPayMode(this.getBillPayMode());
        data.setFinalized(this.finalized);
        data.setInvoiceNumber(this.invoiceNumber);
        return data;
    }

    public static PatientBillItem clone(PatientBillItem pbi) {
        PatientBillItem data = new PatientBillItem();

        if (pbi.getItem() != null) {
            data.setItem(pbi.getItem());
        }
        data.setBillingDate(pbi.getBillingDate());
        data.setQuantity(pbi.getQuantity());
        data.setPrice(pbi.getPrice());
        data.setAmount(pbi.getAmount());
        data.setDiscount(pbi.getDiscount());
        data.setPaid(pbi.getPaid());
        data.setTransactionId(pbi.getTransactionId());
        data.setStatus(pbi.getStatus());
        data.setBillPayMode(pbi.getBillPayMode());
        data.setFinalized(pbi.isFinalized());
        data.setInvoiceNumber(pbi.getInvoiceNumber());
        data.setEntryType(pbi.getEntryType());
        data.setPatientBill(pbi.getPatientBill());
        data.setMedicId(pbi.getMedicId());
        data.setStoreId(pbi.getStoreId());
        data.setRequestReference(pbi.getRequestReference());
        data.setPaymentReference(pbi.getPaymentReference());
        data.setServicePoint(pbi.getServicePoint());
        data.setServicePointId(pbi.getServicePointId());
        data.setScheme(pbi.getScheme());
        return data;
    }

    private Double toDouble(Double val) {
        if (val == null) {
            return 0D;
        }
        return val;
    }

    @Override
    public String toString() {
        return "Patient Bill Item [id=" + getId() + ",patientBill=" + patientBill + " , service point=" + servicePoint + ", quantity=" + quantity + ", price=" + price + ", amount=" + amount + " ]";
    }

    public Double getNetAmount() {
        return ((this.quantity * this.price) - this.discount);
    }
}
