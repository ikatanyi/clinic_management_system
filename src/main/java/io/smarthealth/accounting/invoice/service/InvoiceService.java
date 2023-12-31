package io.smarthealth.accounting.invoice.service;

import io.smarthealth.accounting.accounts.data.FinancialActivity;
import io.smarthealth.accounting.accounts.domain.*;
import io.smarthealth.accounting.accounts.service.JournalService;
import io.smarthealth.accounting.billing.data.BillData;
import io.smarthealth.accounting.billing.data.BillItemData;
import io.smarthealth.accounting.billing.domain.PatientBill;
import io.smarthealth.accounting.billing.domain.PatientBillItem;
import io.smarthealth.accounting.billing.domain.enumeration.BillEntryType;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.accounting.billing.service.PatientBillingService;
import io.smarthealth.accounting.invoice.data.*;
import io.smarthealth.accounting.invoice.data.statement.InterimInvoice;
import io.smarthealth.accounting.invoice.data.statement.InterimInvoiceItem;
import io.smarthealth.accounting.invoice.data.statement.InvoiceStatement;
import io.smarthealth.accounting.invoice.domain.*;
import io.smarthealth.accounting.invoice.domain.specification.InvoiceItemSpecification;
import io.smarthealth.accounting.invoice.domain.specification.InvoiceSpecification;
import io.smarthealth.clinical.visit.domain.PaymentDetails;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;
import io.smarthealth.clinical.visit.service.PaymentDetailsService;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.debtor.payer.domain.Payer;
import io.smarthealth.debtor.payer.domain.Scheme;
import io.smarthealth.debtor.payer.service.PayerService;
import io.smarthealth.debtor.scheme.domain.SchemeConfigurations;
import io.smarthealth.debtor.scheme.service.SchemeService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.smarthealth.report.data.CompanyHeader;
import io.smarthealth.security.util.SecurityUtils;
import io.smarthealth.sequence.SequenceNumberService;
import io.smarthealth.sequence.Sequences;
import io.smarthealth.stat.service.ReportService;
import io.smarthealth.stock.item.domain.enumeration.ItemCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Kelsas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceMergeRepository invoiceMergeRepository;
    private final BillingService billingService;
    private final JournalService journalService;
    private final PayerService payerService;
    private final SchemeService schemeService;
    private final SequenceNumberService sequenceNumberService;
    private final FinancialActivityAccountRepository activityAccountRepository;
    private final VisitService visitService;
    private final PaymentDetailsService paymentDetailsService;
    private final PatientBillingService patientBillingService;
//    private final TxnService txnService;
    private final CompanyHeader companyHeader;
    private final ReportService reportService;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<Invoice> createInvoice(CreateInvoice invoiceData) {
        //TODO:: check if there's already an existing invoices for the current visits, if available create a new supplimentary invoice
        Visit visit = visitService.findVisitEntityOrThrow(invoiceData.getVisitNumber());

        //determine if we already closed invoice
        Optional<PaymentDetails> paymentDetails = paymentDetailsService.getPaymentDetailsByVist(visit);

        String trxId = sequenceNumberService.next(1L, Sequences.Transactions.name());

        List<Invoice> savedInvoices = new ArrayList<>();

        invoiceData.getPayers()
                .stream()
                .forEach(payerData -> {

                    Payer payer = payerService.findPayerByIdWithNotFoundDetection(payerData.getPayerId());
                    Scheme scheme = schemeService.fetchSchemeById(payerData.getSchemeId());

                    List<Invoice> existingInvoice = invoiceRepository.findByVisitAndScheme(visit, scheme);
                    String invoiceNo;
                    //check if we have an existing invoice otherwise create a supplimentary invoice
                    if (existingInvoice.isEmpty()) {
                        invoiceNo = sequenceNumberService.next(1L, Sequences.Invoice.name());
                    } else {
                        Invoice inv = existingInvoice.stream().findAny().get();
                        invoiceNo = inv.getNumber() + "S" + existingInvoice.size();
                    }

                    Optional<SchemeConfigurations> config = schemeService.fetchSchemeConfigByScheme(scheme);

                    Integer creditDays = 30;
                    String terms = "Net 30";
                    if (payer.getPaymentTerms() != null) {
                        creditDays = payer.getPaymentTerms().getCreditDays();
                        terms = payer.getPaymentTerms().getTermsName();
                    }
                    //determining the invoice amount to use 
                    BigDecimal invoiceAmount = payerData.getAmount();
                    BigDecimal discount = invoiceData.getDiscount();
                    Boolean isCapitation = Boolean.FALSE;
                    if (config.isPresent() && config.get().isCapitationEnabled()) {
                        //if this is a capitation invoice we going to mess this shit up
                        if (paymentDetails.isPresent()) {
                            PaymentDetails pd = paymentDetails.get();
                            if(pd.isHasCapitation()){
                                invoiceAmount = pd.getCapitationAmount();
                            }
                        }else {
                            invoiceAmount = config.get().getCapitationAmount();
                        }
                        discount = BigDecimal.ZERO;
                        isCapitation = Boolean.TRUE;
                    }

                    Invoice invoice = new Invoice();
                    invoice.setAmount(invoiceAmount);
                    invoice.setBalance(invoiceAmount);
                    invoice.setCapitation(isCapitation);
                    invoice.setDate(invoiceData.getDate());
                    invoice.setDueDate(invoiceData.getDate().plusDays(creditDays));
                    invoice.setInvoiceAmount(payerData.getAmount());
                    invoice.setDiscount(discount);
                    invoice.setMemberName(payerData.getMemberName());
                    invoice.setMemberNumber(payerData.getMemberNo());
                    invoice.setNotes(invoiceData.getNotes());
                    invoice.setNumber(invoiceNo);
                    invoice.setPaid(Boolean.FALSE);
                    invoice.setPatient(visit.getPatient());
                    invoice.setPayer(payer);
                    invoice.setTerms(terms);
                    invoice.setScheme(scheme);
                    invoice.setStatus(InvoiceStatus.Draft);
                    invoice.setTax(invoiceData.getTaxes());
                    invoice.setTransactionNo(trxId);
                    invoice.setVisit(visit);
                    if (paymentDetails.isPresent()) {
                        invoice.setIdNumber(paymentDetails.get().getIdNo()); // ?? am not sure
                        invoice.setPreauthCode(paymentDetails.get().getPreauthCode());
                    }

                    if (config.isPresent()) {
                        if (config.get().isSmartEnabled()) {
                            invoice.setAwaitingSmart(Boolean.TRUE);
                        }
                    }

                    if (!invoiceData.getItems().isEmpty()) {
                        BigDecimal balance = BigDecimal.ZERO;
                        invoiceData.getItems()
                                .stream()
                                .forEach(inv -> {
                                    PatientBillItem item = billingService.findBillItemById(inv.getBillItemId());
                                    //only finalize what has not been finalized
                                    if (item.isFinalized() == false) {
                                        InvoiceItem lineItem = new InvoiceItem();
                                        BigDecimal bal = BigDecimal.valueOf(item.getAmount()).subtract(inv.getAmount());
                                        item.setPaid(Boolean.TRUE);
                                        item.setStatus(BillStatus.Paid);
                                        if (item.getPaymentReference() == null) {
                                            item.setPaymentReference(invoiceNo);
                                        }

//                                        item.setFinalized(true);
                                        final String finalInv = StringUtils.isNotBlank(item.getInvoiceNumber()) ? StringUtils.join(item.getInvoiceNumber(), invoiceNo, ",") : invoiceNo;
                                        System.err.println("final Invoice "+finalInv);
                                        item.setInvoiceNumber(invoiceNo);
                                        item.setBalance(0D);

                                        PatientBillItem updatedItem = patientBillingService.updateBillItem(item);
                                        lineItem.setBillItem(updatedItem);
//                                            lineItem.setBalance(inv.getAmount().doubleValue() > 0 ? inv.getAmount() : BigDecimal.ZERO);
                                        lineItem.setBalance(inv.getAmount());
                                        if (updatedItem.getItem().getCategory() == ItemCategory.CoPay
                                                || updatedItem.getItem().getCategory() == ItemCategory.Receipt
                                                || updatedItem.getItem().getCategory() == ItemCategory.NHIF_Rebate
                                                || updatedItem.getItem().getCategory() == ItemCategory.Discount
                                                || updatedItem.getBillPayMode() != PaymentMethod.Cash) {
                                            invoice.addItem(lineItem);
                                        }
                                        //this is
                                    }
                                });
                        BigDecimal lineTotals = invoice.getItems().stream()
                                .map(x -> x.getBillItem().getEntryType() == BillEntryType.Debit?  x.getBalance() :x.getBalance().negate())
                                .reduce(BigDecimal.ZERO, (x, y) -> x.add(y));
                        System.err.println("Line Totals: ?>>>> " + lineTotals);
                        if (config.isPresent() && config.get().isCapitationEnabled()) {
                            invoice.setBalance(invoiceAmount);
                        } else {
                            invoice.setBalance(lineTotals);
                        }
                        System.err.println("Calculated Line Totals: ?>>>> " + invoice.getBalance());
                    }
                    savedInvoices.add(saveInvoice(invoice));
                }
                );
        // create an invoice entry to the patient bill as a receipt

        for (Invoice savedInvoice : savedInvoices) {
            patientBillingService.createReceiptItem(visit.getVisitNumber(), savedInvoice);

            savedInvoice.getItems()
                    .stream()
                    .forEach(pt -> {
                        PatientBillItem item = pt.getBillItem();
                        item.setFinalized(true);
                        item.setBalance(0D);
                        patientBillingService.updateBillItem(item);
                    });
        }
        //update the invoice wuth balances from the above

        return savedInvoices;

    }
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Invoice createInvoiceRebate(RebateInvoice invoiceData) {

        //check if patient rebate has already been past
//        select date_part('year',now())||lpad(date_part('month',now())::text,2,'0')||lpad('5',5,'0'),date('now')
//20210100005
        String trxId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        String claimNo = sequenceNumberService.next(1L, Sequences.ClaimNumber.name());

        LocalDate now = LocalDate.now();
//         String month = String.format("%1$" + 2 + "s", now.getMonthValue()).replace(' ', '0');
        String m = StringUtils.leftPad(String.valueOf(now.getMonthValue()), 2, "0");
        claimNo = String.format("%s%s%s", now.getYear(), m, claimNo);

        Scheme scheme = schemeService.fetchSchemeById(invoiceData.getSchemeId());
        Visit visit = visitService.findVisitEntityOrThrow(invoiceData.getVisitNumber());
        BigDecimal invoiceAmount = invoiceData.getAmount();

        Integer creditDays = 30;
        String terms = "Net 30";
        Payer payer = scheme.getPayer();

        if (payer.getPaymentTerms() != null) {
            creditDays = payer.getPaymentTerms().getCreditDays();
            terms = payer.getPaymentTerms().getTermsName();
        }
        //generate the claim number here

        Invoice invoice = new Invoice();
        invoice.setAmount(invoiceAmount);
        invoice.setBalance(invoiceAmount);
        invoice.setCapitation(false);
        invoice.setDate(invoiceData.getDate());
        invoice.setDueDate(invoiceData.getDate().plusDays(creditDays));
        invoice.setInvoiceAmount(invoiceAmount);
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setMemberName(invoiceData.getMemberName());
        invoice.setMemberNumber(invoiceData.getMemberNumber());
        invoice.setNotes(invoiceData.getDescription());
        invoice.setNumber(claimNo);
        invoice.setPaid(Boolean.FALSE);
        invoice.setPatient(visit.getPatient());
        invoice.setPayer(payer);
        invoice.setTerms(terms);
        invoice.setScheme(scheme);
        invoice.setStatus(InvoiceStatus.Draft);
        invoice.setTax(BigDecimal.ZERO);
        invoice.setTransactionNo(trxId);
        invoice.setVisit(visit);
        invoice.setRebate(Boolean.TRUE);
        Invoice savedInvoice = saveInvoice(invoice);
       //
        patientBillingService.createReceiptItem(visit.getPatient().getPatientNumber(), visit.getPatient().getFullName(),visit.getVisitNumber(),savedInvoice.getAmount().doubleValue(),ItemCategory.NHIF_Rebate,false,savedInvoice.getNumber());
        //debt Debtor
        //save the rebates

        return savedInvoice;
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public Optional<Invoice> getInvoiceByNumber(String number) {
        return invoiceRepository.findByNumber(number);
    }
    
    public List<Invoice> getInvoiceByVisit(String visitNumber) {
        return invoiceRepository.findByVisit_VisitNumber(visitNumber);
    }

    //
    public Invoice getInvoiceByIdOrThrow(Long id) {
        return getInvoiceById(id)
                .orElseThrow(() -> APIException.notFound("Invoice with ID {0} not found.", id));
    }

    public Invoice getInvoiceByNumberOrThrow(String number) {
        return getInvoiceByNumber(number)
                .orElseThrow(() -> APIException.notFound("Invoice with Number  {0} not found.", number));
    }

    public Invoice saveInvoice(Invoice invoice) {

        Invoice savedInvoice = invoiceRepository.save(invoice);
        journalService.save(toJournal(invoice));
        return savedInvoice;
    }

    public void updateInvoiceStatus(Long id, InvoiceStatus status) {
        getInvoiceByIdOrThrow(id);
        invoiceRepository.updateInvoiceStatus(status, id);
    }

    public void updateInvoiceSmartStatus(Long id, Boolean awaitingSmart) {
        Invoice invoice = getInvoiceByIdOrThrow(id);
        invoice.setAwaitingSmart(awaitingSmart);
        invoiceRepository.save(invoice);
    }

    public Invoice updateInvoice(Invoice invoice) {
        if (invoice != null) {
            return invoiceRepository.save(invoice);
        }
        return null;
    }

    public Page<Invoice> fetchInvoices(Long payer, Long scheme, String invoice, InvoiceStatus status, String patientNo, DateRange range, Double amountGreaterThan, Boolean filterPastDue, Boolean awaitingSmart, Double amountLessThanOrEqualTo, Boolean hasCapitation, Pageable pageable) {

        Specification<Invoice> spec = InvoiceSpecification.createSpecification(payer, scheme, invoice, status, patientNo, range, amountGreaterThan, filterPastDue, awaitingSmart, amountLessThanOrEqualTo, hasCapitation);
        Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
//        Page<Invoice> invoices = invoiceRepository.findByItemsVoidedFalse(spec, pageable);

        return invoices;
    }

    public Page<Invoice> searchInvoice(String term, InvoiceStatus status, Pageable pageable) {
        Specification<Invoice> spec = InvoiceSpecification.searchSpecification(term, status);
        Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
        return invoices;
    }

    public Page<InvoiceItem> fetchVoidedInvoiceItem(Long payer, Long scheme, String invoice, InvoiceStatus status, String patientNo, DateRange range, Pageable page) {

        Specification<InvoiceItem> spec = InvoiceItemSpecification.createSpecification(payer, scheme, invoice, status, patientNo, range);
        Page<InvoiceItem> invoices = invoiceItemRepository.findAll(spec, page);

        return invoices;
    }

    public InvoiceItem getInvoiceItemByIdOrThrow(Long id) {
        return invoiceItemRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("InvoiceItem with ID {0} not found.", id));
    }

    public Invoice updateInvoice(Long id, InvoiceEditData data) {
        Invoice invoice = getInvoiceByIdOrThrow(id);
        Payer payer = payerService.findPayerByIdWithNotFoundDetection(data.getPayerId());
        Scheme scheme = schemeService.fetchSchemeById(data.getSchemeId());

//        Optional<SchemeConfigurations> config = schemeService.fetchSchemeConfigByScheme(scheme);
        //determining the invoice amount to use 
        invoice.setMemberNumber(data.getMemberNumber());
        invoice.setMemberName(data.getMemberName());
        invoice.setNotes(data.getNotes());
        invoice.setPayer(payer);
        invoice.setScheme(scheme);
        return invoiceRepository.save(invoice);
    }

//    @Transactional
//    public InvoiceMerge mergedInvoiceProcee(InvoiceMergeData data) {
//        InvoiceMerge invoiceMerge = InvoiceMergeData.map(data);
//
//        Invoice fromInvoice = getInvoiceByNumberOrThrow(data.getFromInvoiceNumber());
//        Invoice toInvoice = getInvoiceByNumberOrThrow(data.getToInvoiceNumber());
//
//        fromInvoice.getItems().stream().map((lineItem) -> {
//            lineItem.setInvoice(toInvoice);
//            return lineItem;
//        }).forEachOrdered((lineItem) -> {
//            toInvoice.getItems().add(lineItem);
//        });
//
//        invoiceRepository.save(toInvoice);
//        invoiceRepository.deleteById(fromInvoice.getId());
//        return invoiceMergeRepository.save(invoiceMerge);
//    }
    //TODO cancelling of the invoice
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Invoice cancelInvoice(String invoiceNo, List<InvoiceItemData> items) {
        Invoice invoice = invoiceRepository.findByNumber(invoiceNo).orElseThrow(() -> APIException.notFound("Invoice Number {0} not found", invoiceNo));

        BigDecimal oldAmount = invoice.getBalance();

        List<InvoiceItem> lists
                = items.stream()
                        .map(x -> {
//                            PatientBillItem item = billingService.findBillItemById(x.getItemId());
                            Optional<InvoiceItem> invoiceItem = invoiceItemRepository.findById(x.getId());
                            if (invoiceItem.isPresent()) {
                                InvoiceItem iv = invoiceItem.get();
                                iv.setVoided(Boolean.TRUE);
                                iv.setRemarks(x.getRemarks());
                                iv.setVoidedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
                                iv.setVoidedDatetime(LocalDateTime.now());
                                BigDecimal newAmt = invoice.getAmount().subtract(iv.getBalance());
                                BigDecimal bal = invoice.getBalance().subtract(iv.getBalance());
                                invoice.setBalance(bal);
                                invoice.setAmount(newAmt);
                                return iv;
                            }
                            return null;
                        })
                        .filter(bill -> bill != null)
                        .collect(Collectors.toList());

        invoiceItemRepository.saveAll(lists);
        //if items provided cancel the said items
        //otherwise cancel the entire
        BigDecimal amount = lists.stream().map(x -> x.getBalance()).reduce(BigDecimal.ZERO, (x, y) -> x.add(y));
        BigDecimal adjustedAmount = oldAmount.subtract(amount);
        reverseInvoiceItem(invoice, adjustedAmount);

        // post the journal
        return new Invoice();
    }

//    public Invoice findInvoiceOrThrowException(Long id) {
//        return findById(id)
//                .orElseThrow(() -> APIException.notFound("Invoice with id {0} not found.", id));
//    }
    public String emailInvoice(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public InvoiceData invoiceToEDI(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private JournalEntry toJournal(Invoice invoice) {

        Account debitAccount;
        if (invoice.getPayer().getDebitAccount() != null) {
            debitAccount = invoice.getPayer().getDebitAccount();
        } else {
            FinancialActivityAccount debit = activityAccountRepository
                    .findByFinancialActivity(FinancialActivity.Accounts_Receivable)
                    .orElseThrow(() -> APIException.notFound("Account Receivable Account is Not Mapped"));
            debitAccount = debit.getAccount();
        }
        FinancialActivityAccount creditAccount = activityAccountRepository
                .findByFinancialActivity(FinancialActivity.Patient_Control)
                .orElseThrow(() -> APIException.notFound("Patient Control Account is Not Mapped"));

//        String creditAcc = creditAccount.getAccount().getIdentifier();
//        String debitAcc = debitAccount.getIdentifier();
        BigDecimal amount = invoice.getAmount();
        BigDecimal debitAmount = amount;
        BigDecimal creditAmount = amount;
        Optional<SchemeConfigurations> config = schemeService.fetchSchemeConfigByScheme(invoice.getScheme());
        List<JournalEntryItem> capitationJournal = new ArrayList<>();
        if (config.isPresent() && config.get().isCapitationEnabled()) {
            BigDecimal capitationAmount = config.get().getCapitationAmount();
            //capitations export the       3800-7000
            BigDecimal capitationDiff = (capitationAmount.subtract(invoice.getInvoiceAmount()));
            switch (capitationDiff.signum()) {
                case -1: {
                    //negative - expense
                    FinancialActivityAccount capitationAccount = activityAccountRepository
                            .findByFinancialActivity(FinancialActivity.CapitationExpense)
                            .orElseThrow(() -> APIException.notFound("Capitation Expense Account is Not Mapped"));
                    //deferrence is what I post here otherwise 
                    debitAmount = capitationAmount;
                    creditAmount = invoice.getInvoiceAmount();

                    JournalEntryItem capitationExp = new JournalEntryItem(capitationAccount.getAccount(), "Capitation Expense for Invoice No. " + invoice.getNumber(), (capitationDiff.negate()), BigDecimal.ZERO);
                    capitationJournal.add(capitationExp);
                }
                break;
                case 1: {
                    FinancialActivityAccount capitationAccount = activityAccountRepository
                            .findByFinancialActivity(FinancialActivity.CapitationIncome)
                            .orElseThrow(() -> APIException.notFound("Capitation Income Account is Not Mapped"));
                    //deferrence is what I post here otherwise 
                    debitAmount = capitationAmount;
                    creditAmount = invoice.getInvoiceAmount();

                    JournalEntryItem capitationIncomel = new JournalEntryItem(capitationAccount.getAccount(), "Capitation Income  for Invoice No. " + invoice.getNumber(), BigDecimal.ZERO, capitationDiff);
                    capitationJournal.add(capitationIncomel);
                }
                break;
                default:
            }

        }
        String narration = "Raise Invoice - " + invoice.getNumber();
        capitationJournal.add(new JournalEntryItem(debitAccount, narration, debitAmount, BigDecimal.ZERO));
        capitationJournal.add(new JournalEntryItem(creditAccount.getAccount(), narration, BigDecimal.ZERO, creditAmount));

        JournalEntry toSave = new JournalEntry(invoice.getDate(), narration, capitationJournal);

        toSave.setTransactionNo(invoice.getTransactionNo());
        toSave.setTransactionType(TransactionType.Invoicing);
        toSave.setStatus(JournalState.PENDING);
        return toSave;
    }

    private JournalEntry reverseInvoiceItem(Invoice invoice, BigDecimal amount) {

        Account reverseCredit;
        if (invoice.getPayer().getDebitAccount() != null) {
            reverseCredit = invoice.getPayer().getDebitAccount();
        } else {
            FinancialActivityAccount debit = activityAccountRepository
                    .findByFinancialActivity(FinancialActivity.Accounts_Receivable)
                    .orElseThrow(() -> APIException.notFound("Account Receivable Account is Not Mapped"));
            reverseCredit = debit.getAccount();
        }
        FinancialActivityAccount reverseDebit = activityAccountRepository
                .findByFinancialActivity(FinancialActivity.Patient_Control)
                .orElseThrow(() -> APIException.notFound("Patient Control Account is Not Mapped"));

        String narration = "Invoice Bill Reversal - " + invoice.getNumber();
        JournalEntry toSave = new JournalEntry(invoice.getDate(), narration,
                new JournalEntryItem[]{
                    new JournalEntryItem(reverseDebit.getAccount(), narration, amount, BigDecimal.ZERO),
                    new JournalEntryItem(reverseCredit, narration, BigDecimal.ZERO, amount)
                }
        );
        toSave.setTransactionNo(invoice.getTransactionNo());
        toSave.setTransactionType(TransactionType.Bill_Reversal);
        toSave.setStatus(JournalState.PENDING);
        return toSave;
    }

    @Transactional
    public Invoice addInvoiceItem(String invoiceNumber, List<BillItemData> invoiceItems) {
        Invoice invoice = getInvoiceByNumberOrThrow(invoiceNumber);
        // create the patient bill and then invoice them
        Double amount = invoiceItems.stream()
                .map(x -> x.getAmount())
                .reduce(0D, (x, y) -> x + y);

        BillData billdata = new BillData();
        billdata.setVisitNumber(invoice.getVisit().getVisitNumber());
        billdata.setBillingDate(invoice.getDate());
        billdata.setPaymentMode("Insurance");
        billdata.setWalkinFlag(Boolean.FALSE);
        billdata.setBillItems(invoiceItems);
        billdata.setAmount(amount);

        PatientBill patientBill = billingService.createPatientBill(billdata);
        //then finalize this bill
        patientBill.getBillItems()
                .stream()
                .forEach(item -> {

                    InvoiceItem lineItem = new InvoiceItem();
                    item.setPaid(Boolean.TRUE);
                    item.setStatus(BillStatus.Paid);
                    item.setPaymentReference(invoice.getNumber());
                    item.setBalance(0D);
                    PatientBillItem updatedItem = billingService.updateBillItem(item);
                    lineItem.setBillItem(updatedItem);
//                lineItem.setBalance(inv.getAmount().doubleValue() > 0 ? inv.getAmount() : BigDecimal.ZERO);
                    lineItem.setBalance(BigDecimal.valueOf(item.getAmount()));
                    invoice.addItem(lineItem);
                });

        BigDecimal addBal = patientBill.getBillItems()
                .stream().map(x -> BigDecimal.valueOf(0))
                .reduce(BigDecimal.ZERO, (x, y) -> x.add(y));

        invoice.setBalance(invoice.getBalance().add(addBal));
        //recalculate the new balance
        Invoice savedInvoice = invoiceRepository.save(invoice);
        //to journal the amendments
        Account debitAccount;
        if (invoice.getPayer().getDebitAccount() != null) {
            debitAccount = invoice.getPayer().getDebitAccount();
        } else {
            FinancialActivityAccount debit = activityAccountRepository
                    .findByFinancialActivity(FinancialActivity.Accounts_Receivable)
                    .orElseThrow(() -> APIException.notFound("Account Receivable Account is Not Mapped"));
            debitAccount = debit.getAccount();
        }
        FinancialActivityAccount creditAccount = activityAccountRepository
                .findByFinancialActivity(FinancialActivity.Patient_Control)
                .orElseThrow(() -> APIException.notFound("Patient Control Account is Not Mapped"));

        BigDecimal amt = BigDecimal.valueOf(amount);

        String narration = "Amended Invoice  - " + invoice.getNumber();
        JournalEntry toSave = new JournalEntry(invoice.getDate(), narration,
                new JournalEntryItem[]{
                    new JournalEntryItem(debitAccount, narration, amt, BigDecimal.ZERO),
                    new JournalEntryItem(creditAccount.getAccount(), narration, BigDecimal.ZERO, amt)
                }
        );
        toSave.setTransactionNo(invoice.getTransactionNo());
        toSave.setTransactionType(TransactionType.Invoicing);
        toSave.setStatus(JournalState.PENDING);

        journalService.save(toSave);
        return savedInvoice;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Invoice mergeInvoice(MergeInvoice mergeInvoice) {
        //check if any of the invoice is dispatched
        Invoice invoice = getInvoiceByNumberOrThrow(mergeInvoice.getInvoiceNo());
        if (invoice.getStatus() == InvoiceStatus.Sent) {
            throw APIException.badRequest("Invoice {0} is already dispatched to payer, Merging is not possible", mergeInvoice.getInvoiceNo());
        }
        Invoice toInvoice = getInvoiceByNumberOrThrow(mergeInvoice.getInvoiceToMerge());

        if (toInvoice.getStatus() == InvoiceStatus.Sent) {
            throw APIException.badRequest("Invoice {0} is already dispatched to payer, Merging is not possible", mergeInvoice.getInvoiceToMerge());
        }

        toInvoice.getItems().stream()
                .map(x -> {
                    x.setInvoice(invoice);
                    return x;
                })
                .forEachOrdered(inv -> {
                    invoice.getItems().add(inv);
                });
        //updatet the bills
        billingService.updatePaymentReference(toInvoice.getNumber(), invoice.getNumber());
        //delete the old invoice
//        invoiceRepository.deleteById(toInvoice.getId()); 
        toInvoice.setStatus(InvoiceStatus.Voided);
        toInvoice.setAmount(BigDecimal.ZERO);
        toInvoice.setBalance(BigDecimal.ZERO);
        invoiceRepository.save(toInvoice);
        //audi log the changes
        //String fromInvoiceNumber, String toInvoiceNumber, BigDecimal originalInvoiceAmount, BigDecimal newInvoiceAmount, String reasonForMerge

        Double amount = invoice.getItems().stream()
                .map(x -> {
                    return x.getBillItem().getAmount();
                })
                .reduce(0D, (x, y) -> x + y);

//        Double balance = invoice.getItems().stream()
//                .map(x -> {
//                    return x.getBillItem().getBalance();
//                })
//                .reduce(0D, (x, y) -> x + y);
        BigDecimal originalAmount = invoice.getAmount();
        invoice.setAmount(BigDecimal.valueOf(amount));
        invoice.setBalance(BigDecimal.valueOf(amount));

        invoiceMergeRepository.save(new InvoiceMerge(toInvoice.getNumber(), invoice.getNumber(), originalAmount, invoice.getAmount(), mergeInvoice.getReason()));

        return invoiceRepository.save(invoice);
    }

   public InterimInvoice getInterimInvoiceStatement(String visitNumber, String type){
        Optional<Visit> optionalVisit = visitService.findVisit(visitNumber);

        if(optionalVisit.isPresent()){
            InterimInvoice invoice = new InterimInvoice();
            Visit visit = optionalVisit.get();
            invoice.setPaymentMode(visit.getPaymentMethod().name());
            if(visit.getPaymentDetails()!=null){
                Payer payer = visit.getPaymentDetails().getPayer();
                Scheme scheme = visit.getPaymentDetails().getScheme();
                invoice.setPayerId(payer.getId());
                invoice.setPayerName(payer.getPayerName());
                invoice.setSchemeId(scheme.getId());
                invoice.setSchemeName(scheme.getSchemeName());
                invoice.setMemberName(visit.getPaymentDetails().getMemberName());
                invoice.setMemberNumber(visit.getPaymentDetails().getPolicyNo());
                invoice.setPaymentTerms(payer.getPaymentTerms()!=null ? payer.getPaymentTerms().getTermsName(): "Due Immediately");
                invoice.setCreditDays(payer.getPaymentTerms()!=null ? payer.getPaymentTerms().getCreditDays() : 0);
            }

            invoice.setInvoiceNumber(visitNumber);
            invoice.setVisitDate(visit.getStartDatetime());
            invoice.setVisitType(visit.getVisitType()!=null ? visit.getVisitType().name() : "");
            invoice.setPatientNumber(visit.getPatient().getPatientNumber());
            invoice.setPatientName(visit.getPatient().getFullName());
            invoice.setItems(
                    patientBillingService.getInterimBillItems(visitNumber, type).stream()
                            .filter(x -> x.getEntryType() == BillEntryType.Debit)
                    .map(InterimInvoiceItem::of)
                    .collect(Collectors.toList())
            );

            invoice.setCreditItems(
                    patientBillingService.getInterimBillItems(visitNumber, type).stream()
                            .filter(x -> x.getEntryType() == BillEntryType.Credit)
                            .map(InterimInvoiceItem::of)
                            .collect(Collectors.toList())
            );
            return invoice;
        }
        return new InterimInvoice();
   }

   public InvoiceStatement getInvoiceStatement(String invoiceNumber){
        Optional<Invoice> optionalInvoice = invoiceRepository.findByNumber(invoiceNumber);
        if(optionalInvoice.isPresent()){
            return InvoiceStatement.of(optionalInvoice.get());
        }
        return new InvoiceStatement();
   }

   public JasperPrint generateInterimStatement(String visitNumber, String type) throws FileNotFoundException, JRException {
        if(type == null ) type ="standard";
       List<CompanyHeader> header = Arrays.asList(companyHeader);
       List<InterimInvoice> invoices = Arrays.asList(getInterimInvoiceStatement(visitNumber, type));

       System.err.println("Invoice Net Amount: "+invoices.get(0).getNetAmount());
       type=  StringUtils.capitalize(type.toLowerCase());
       String reportTemplate = String.format("classpath:reports/accounts/invoice/%sInterimStatement.jrxml", type);
       File file = ResourceUtils.getFile(reportTemplate);
       JasperReport jasperReport = JasperCompileManager.compileReport(file.getAbsolutePath());
       JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(invoices);
       Map<String, Object> parameters = new HashMap<>();
       parameters.put("CompanyHeader", header);
       JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
       return jasperPrint;
   }

   public byte[] generateInterimStatement(ExportFormat exportFormat, String visitNumber, String type){
       if(type == null ) type ="standard";

       type=  StringUtils.capitalize(type.toLowerCase());
       String inputFileName = String.format("%sInterimStatement", type);

       List<CompanyHeader> header = Arrays.asList(companyHeader);
       Map<String, Object> parameters = new HashMap<>();
       parameters.put("CompanyHeader", header);

       List<InterimInvoice> invoices = Arrays.asList(getInterimInvoiceStatement(visitNumber, type));
       JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(invoices);

       return reportService.generateReport(exportFormat,inputFileName,parameters,dataSource);
   }

}
