package io.smarthealth.accounting.invoice.service;

import io.smarthealth.accounting.billing.domain.Bill;
import io.smarthealth.accounting.billing.domain.BillItem;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.accounting.invoice.data.CreateInvoiceData;
import io.smarthealth.accounting.invoice.data.CreateInvoiceItemData;
import io.smarthealth.accounting.invoice.data.InvoiceData;
import io.smarthealth.accounting.invoice.data.InvoiceMergeData;
import io.smarthealth.accounting.invoice.domain.Invoice;
import io.smarthealth.accounting.invoice.domain.InvoiceRepository;
import io.smarthealth.accounting.invoice.domain.InvoiceLineItem;
import io.smarthealth.accounting.invoice.domain.InvoiceMerge;
import io.smarthealth.accounting.invoice.domain.InvoiceMergeRepository;
import io.smarthealth.accounting.invoice.domain.InvoiceStatus;
import io.smarthealth.accounting.invoice.domain.specification.InvoiceSpecification;
import io.smarthealth.debtor.payer.domain.Payer;
import io.smarthealth.debtor.payer.domain.Scheme;
import io.smarthealth.debtor.payer.service.PayerService;
import io.smarthealth.debtor.scheme.service.SchemeService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.sequence.service.TxnService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Kelsas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMergeRepository invoiceMergeRepository;
    private final BillingService billingService;
    private final PayerService payerService;
    private final SchemeService schemeService;
    private final TxnService txnService;

    @Transactional
    public String createInvoice(CreateInvoiceData invoiceData) {

        final String trxId = txnService.nextId();// UUID.randomUUID().toString();
//        final 
        Optional<Bill> bill = billingService.findByBillNumber(invoiceData.getBillNumber());
        invoiceData.getPayers()
                .stream()
                .forEach(debt -> {
                    String invoiceNo = RandomStringUtils.randomNumeric(5);
                    Payer payer = payerService.findPayerByIdWithNotFoundDetection(debt.getPayerId());
                    Scheme scheme = schemeService.fetchSchemeById(debt.getSchemeId());

                    Integer creditDays = 30;
                    String terms = "Net 30";
                    if (payer.getPaymentTerms() != null) {
                        creditDays = payer.getPaymentTerms().getCreditDays();
                        terms = payer.getPaymentTerms().getTermsName();
                    }

                    Invoice invoice = new Invoice();
                    invoice.setPayer(payer);
                    invoice.setPayee(scheme.getSchemeName());
                    invoice.setReference(debt.getMemberNo());
                    invoice.setTransactionNo(trxId);
                    invoice.setDate(invoiceData.getDate());
                    invoice.setNotes(invoice.getNotes());

                    invoice.setDueDate(invoiceData.getDate().plusDays(creditDays));
                    invoice.setTerms(terms);

                    invoice.setNumber(invoiceNo);
                    invoice.setSubtotal(debt.getAmount());
                    invoice.setBalance(debt.getAmount());
                    invoice.setDisounts(invoiceData.getDiscount());
                    invoice.setTaxes(invoiceData.getTaxes());
                    invoice.setTotal(debt.getAmount());
                    invoice.setPaid(Boolean.FALSE);
                    invoice.setClosed(Boolean.FALSE);
                    invoice.setDraft(Boolean.FALSE);
                    invoice.setStatus(InvoiceStatus.pending);

                    if (bill.isPresent()) {
                        invoice.setBill(bill.get());

                    }

                    if (!invoiceData.getItems().isEmpty()) {
                        List<InvoiceLineItem> items = invoiceData.getItems()
                                .stream()
                                .map(inv -> createItem(trxId, inv))
                                .collect(Collectors.toList());

                        invoice.addItems(items);
                    }
                    saveInvoice(invoice);
                }
                );
        return trxId;

    }

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        Invoice savedInv = invoiceRepository.save(invoice);
        if (savedInv.getBill() != null) {
            Bill bill = savedInv.getBill();
            bill.setStatus(BillStatus.Final);
            billingService.save(bill);
        }
        return savedInv;
    }

    private InvoiceLineItem createItem(String trxId, CreateInvoiceItemData data) {
        InvoiceLineItem lineItem = new InvoiceLineItem();
        BillItem item = billingService.findBillItemById(data.getBillItemId());
        item.setStatus(BillStatus.Final);

        lineItem.setBillItem(item);
        lineItem.setDeleted(false);

        lineItem.setTransactionId(trxId);
        return lineItem;
    }


    public Optional<Invoice> findById(final Long id) {
        return invoiceRepository.findById(id);
    }

    public Optional<Invoice> findByInvoiceNumber(final String invoiceNo) {
        return invoiceRepository.findByNumber(invoiceNo);
    }

    public Page<Invoice> fetchInvoices(String customer, String invoice, String status, Pageable pageable) {
        InvoiceStatus state = null;
        if (state != null) {
            state = InvoiceStatus.valueOf(status);
        }
        Specification<Invoice> spec = InvoiceSpecification.createSpecification(customer, invoice, state);
        Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
        return invoices;
    }

    public Invoice updateInvoice(Long id, InvoiceData data) {
        Invoice invoice = findInvoiceOrThrowException(id);
        invoice.setBalance(data.getBalance());
        invoice.setClosed(data.getClosed());
        invoice.setCurrency(data.getCurrency());
        invoice.setDisounts(data.getDisounts());
        invoice.setDraft(data.getDraft());
        invoice.setDueDate(data.getDueDate());
        invoice.setNotes(data.getNotes());
        invoice.setPaid(data.getPaid());
        Payer payer = payerService.findPayerByIdWithNotFoundDetection(data.getPayerId());
        invoice.setPayee(data.getPayee());
        invoice.setPayer(payer);
        invoice.setReference(data.getReference());
        invoice.setStatus(data.getStatus());
        invoice.setSubtotal(data.getSubtotal());
        invoice.setTaxes(data.getTaxes());
        invoice.setTerms(data.getTerms());
        invoice.setTotal(data.getTotal());
        return invoiceRepository.save(invoice);
    }
    
    public Invoice verifyInvoice(Long id, Boolean isVerified) {
        Invoice invoice = findInvoiceOrThrowException(id);
        invoice.setIsVerified(isVerified);
        return invoiceRepository.save(invoice);
    }
    
     public Invoice findInvoiceByIdWithFailDetection(String invoiceNumber) {
        return invoiceRepository.findByNumber(invoiceNumber).orElseThrow(() -> APIException.notFound("Invoice with id {0} not found.", invoiceNumber));
    }
     
    
     @javax.transaction.Transactional
    public InvoiceMerge mergeInvoice(InvoiceMergeData data) {
        InvoiceMerge invoiceMerge = InvoiceMergeData.map(data);
        
        Invoice fromInvoice = findInvoiceByIdWithFailDetection(data.getFromInvoiceNumber());
        Invoice toInvoice = findInvoiceByIdWithFailDetection(data.getToInvoiceNumber());
        
        fromInvoice.getItems().stream().map((lineItem) -> {
            lineItem.setInvoice(toInvoice);
            return lineItem;
        }).forEachOrdered((lineItem) -> {
            toInvoice.getItems().add(lineItem);
        });
        
        invoiceRepository.save(toInvoice);
        invoiceRepository.deleteById(fromInvoice.getId());
        return invoiceMergeRepository.save(invoiceMerge);
    }

    public Invoice findInvoiceOrThrowException(Long id) {
        return findById(id)
                .orElseThrow(() -> APIException.notFound("Invoice with id {0} not found.", id));
    }

    public String emailInvoice(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public InvoiceData invoiceToEDI(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
