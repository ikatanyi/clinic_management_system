package io.smarthealth.accounting.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.smarthealth.accounting.accounts.data.FinancialActivity;
import io.smarthealth.accounting.accounts.domain.*;
import io.smarthealth.accounting.accounts.service.JournalService;
import io.smarthealth.accounting.billing.data.*;
import io.smarthealth.accounting.billing.data.nue.BillDetail;
import io.smarthealth.accounting.billing.data.nue.BillItem;
import io.smarthealth.accounting.billing.data.nue.BillItemListToDataConverter;
import io.smarthealth.accounting.billing.data.nue.BillPayment;
import io.smarthealth.accounting.billing.domain.PatientBill;
import io.smarthealth.accounting.billing.domain.PatientBillItem;
import io.smarthealth.accounting.billing.domain.PatientBillItemRepository;
import io.smarthealth.accounting.billing.domain.PatientBillRepository;
import io.smarthealth.accounting.billing.domain.enumeration.BillEntryType;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.domain.enumeration.ExcessAmountPayMethod;
import io.smarthealth.accounting.billing.domain.specification.BillingSpecification;
import io.smarthealth.accounting.billing.domain.specification.PatientBillSpecification;
import io.smarthealth.accounting.doctors.domain.DoctorInvoice;
import io.smarthealth.accounting.doctors.domain.DoctorItem;
import io.smarthealth.accounting.doctors.service.DoctorInvoiceService;
import io.smarthealth.accounting.payment.data.BillReceiptedItem;
import io.smarthealth.accounting.payment.data.ReceivePayment;
import io.smarthealth.accounting.payment.domain.Receipt;
import io.smarthealth.accounting.payment.domain.enumeration.ReceiptType;
import io.smarthealth.accounting.payment.domain.repository.ReceiptRepository;
import io.smarthealth.accounting.pricelist.domain.PriceList;
import io.smarthealth.accounting.pricelist.service.PricelistService;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.clinical.pharmacy.domain.DispensedDrug;
import io.smarthealth.clinical.pharmacy.domain.DispensedDrugRepository;
import io.smarthealth.clinical.pharmacy.service.DispensingService;
import io.smarthealth.clinical.visit.data.enums.VisitEnum;
import io.smarthealth.clinical.visit.domain.PaymentDetails;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.VisitRepository;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;
import io.smarthealth.clinical.visit.service.PaymentDetailsService;
import io.smarthealth.debtor.payer.domain.Scheme;
import io.smarthealth.debtor.scheme.domain.SchemeConfigurations;
import io.smarthealth.debtor.scheme.domain.enumeration.CoPayType;
import io.smarthealth.debtor.scheme.service.SchemeService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.security.util.SecurityUtils;
import io.smarthealth.sequence.SequenceNumberService;
import io.smarthealth.sequence.Sequences;
import io.smarthealth.stock.inventory.domain.StockEntry;
import io.smarthealth.stock.inventory.domain.enumeration.MovementPurpose;
import io.smarthealth.stock.inventory.domain.enumeration.MovementType;
import io.smarthealth.stock.inventory.events.InventoryEvent;
import io.smarthealth.stock.inventory.service.InventoryService;
import io.smarthealth.stock.item.domain.Item;
import io.smarthealth.stock.item.domain.enumeration.ItemCategory;
import io.smarthealth.stock.item.domain.enumeration.ItemType;
import io.smarthealth.stock.item.service.ItemService;
import io.smarthealth.stock.stores.domain.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kelsas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final ServicePointService servicePointService;
    private final PatientBillRepository patientBillRepository;
    private final PatientBillItemRepository billItemRepository;
    private final VisitRepository visitRepository;
    private final SchemeService schemeService;
    private final ItemService itemService;
    private final JournalService journalService;
    private final SequenceNumberService sequenceNumberService;
    private final FinancialActivityAccountRepository activityAccountRepository;
    private final DoctorInvoiceService doctorInvoiceService;
    private final CashPaidUpdater cashPaidUpdater;
    private final PaymentDetailsService paymentDetailsService;
    private final PricelistService pricelistService;
    private final ReceiptRepository receiptRepository;
    private final InventoryService inventoryService;
    private final DispensedDrugRepository dispensedDrugRepository;

    public static <T> java.util.function.Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    //Create service bill
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PatientBill createPatientBill(BillData data) {
        PatientBill patientbill = new PatientBill();
        Visit visit = null;
        if (!data.getWalkinFlag()) {
            visit = findVisitEntityOrThrow(data.getVisitNumber());
            patientbill.setVisit(visit);
            patientbill.setPatient(visit.getPatient());
            patientbill.setWalkinFlag(Boolean.FALSE);
        } else {
            patientbill.setReference(data.getPatientNumber());
            patientbill.setOtherDetails(data.getPatientName());
            patientbill.setWalkinFlag(Boolean.TRUE);
        }

        patientbill.setAmount(data.getAmount());
        patientbill.setDiscount(data.getDiscount());
        patientbill.setBalance(data.getAmount());

        patientbill.setBillingDate(data.getBillingDate());
        patientbill.setPaymentMode(data.getPaymentMode());

        patientbill.setStatus(BillStatus.Draft);

        String trdId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

        patientbill.setBillNumber(bill_no);
        patientbill.setTransactionId(trdId);

        List<PatientBillItem> lineItems = data.getBillItems()
                .stream()
                .map(lineData -> {
                    Item item = itemService.findItemWithNoFoundDetection(lineData.getItemCode());
                    PatientBillItem billItem = new PatientBillItem();

                    billItem.setBillingDate(data.getBillingDate());
                    billItem.setTransactionId(trdId);
                    billItem.setItem(item);
                    billItem.setPaid(data.getPaymentMode().equals("Insurance"));
                    billItem.setPrice(lineData.getPrice());
                    if (item.getCategory().equals(ItemCategory.CoPay)) {
                        billItem.setPrice(data.getAmount());
                    }
                    billItem.setQuantity(lineData.getQuantity());
                    System.out.println("billItem.getPrice() " + billItem.getPrice());
                    //SIMON UPDATED HERE TO CHECK NULL BECAUSE REFERAL BILL WAS FAILING
                    if (billItem.getPrice() != null) {
                        billItem.setAmount((billItem.getPrice() * billItem.getQuantity()));
                    } else {
                        billItem.setAmount(lineData.getAmount());
                    }
                    billItem.setDiscount(lineData.getDiscount());
                    billItem.setBalance((billItem.getAmount()));

                    billItem.setServicePoint(lineData.getServicePoint());
                    billItem.setServicePointId(lineData.getServicePointId());
                    billItem.setStatus(BillStatus.Draft);
                    billItem.setMedicId(lineData.getMedicId());
                    billItem.setBillPayMode(lineData.getPaymentMethod());
                    billItem.setEntryType(BillEntryType.Debit);

                    return billItem;
                })
                .collect(Collectors.toList());
        patientbill.addBillItems(lineItems);
        System.out.println("End of bill items");

        PatientBill savedBill = save(patientbill);
        //TODO consider the inpatient billing
        if (savedBill.getPaymentMode().equals("Insurance") || (visit != null && visit.getVisitType() == VisitEnum.VisitType.Inpatient)) {
            journalService.save(toJournal(savedBill, null));
        }
        return savedBill;
    }

    public void createPatientBill(PatientBill bill, Store store) {
        String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());
        bill.setBillNumber(bill_no);
        PatientBill savedBill = save(bill);
        if (savedBill.getPaymentMode().equals("Insurance") || (savedBill.getVisit() != null && savedBill.getVisit().getVisitType() == VisitEnum.VisitType.Inpatient)) {
            journalService.save(toJournal(savedBill, store));
        }
    }

    public PatientBill createPatientBill(PatientBill patientBill) {
        PatientBill savedBill = patientBillRepository.saveAndFlush(patientBill);
        //TODO consider the inpatient billing
        if (savedBill.getPaymentMode().equals("Insurance") || (savedBill.getVisit() != null && savedBill.getVisit().getVisitType() == VisitEnum.VisitType.Inpatient)) {
            journalService.save(toJournal(savedBill, null));
        }
        return savedBill;
    }

    public PatientBill save(PatientBill bill) {
        log.debug("START save bill");
        Optional<PaymentDetails> pd = null;

        if (bill.getVisit() != null) {
            pd = paymentDetailsService.fetchPaymentDetailsByVisitWithoutNotFoundDetection(bill.getVisit());
        }
        double amountToBill = 0.00;
        int itemCount = 0;
        //TODO:: refactor scheme limit checks into a method

        //update the scheme that paid this bill if insurance
        for (PatientBillItem b : bill.getBillItems()) {
            //ignore for now the receipts and copay from limit checks
            if (b.getItem().getCategory() != ItemCategory.CoPay || b.getItem().getCategory() != ItemCategory.Receipt) {
                amountToBill = amountToBill + b.getAmount();
                itemCount++;
            }
            if (bill.getVisit() != null) {
                //Billing paymode added to accomodate exclusions and other functionalities
//                b.setBillPayMode(bill.getVisit().getPaymentMethod());
                if (bill.getVisit().getPaymentMethod().equals(PaymentMethod.Insurance) && pd.isPresent()) {
                    b.setScheme(pd.get().getScheme());
                }
            }
        }

        log.debug("START validate limit amount");

        if (bill.getVisit() != null) {
            pd = paymentDetailsService.fetchPaymentDetailsByVisitWithoutNotFoundDetection(bill.getVisit());
            PaymentDetails payDetails = pd.orElse(null);
            if (payDetails != null) {
                Optional<SchemeConfigurations> schemeConfigurations = payDetails.getScheme() != null ? schemeService.fetchSchemeConfigByScheme(payDetails.getScheme()) : Optional.empty();

                if (schemeConfigurations.isPresent() && schemeConfigurations.get().isLimitEnabled()) {
                    if (payDetails.getRunningLimit() < amountToBill && !payDetails.getExcessAmountEnabled()
                            && bill.getVisit().getVisitType().equals(VisitEnum.VisitType.Outpatient)) {
                        if (bill.getAllowedToExceedLimit().equals(Boolean.FALSE)) {
                            List<BillItemData> billItemData = new ArrayList<>();
                            LimitExceedingResponse response = new LimitExceedingResponse(LimitResponseStatus.ERROR,
                                    "Bill amount (" + amountToBill +
                                            ") exceed running limit amount (" + payDetails.getRunningLimit() + ") ",
                                    bill.getVisit().getVisitNumber(), bill.getPatient().getPatientNumber(),
                                    amountToBill - payDetails.getRunningLimit(), amountToBill,
                                    payDetails.getRunningLimit(),
                                    BillItemListToDataConverter.billItemDataConverter(bill.getBillItems()));
                            throw APIException.limitExceedResponse(response);
                        }
                        if (bill.getAllowedToExceedLimit().equals(Boolean.TRUE) && bill.getBillItems().size() > 1) {
                            throw APIException.badRequest("Bill amount (" + amountToBill + ") exceed \nrunning " +
                                    "limit  amount (" + payDetails.getRunningLimit() + "). \nRemove one or more items" +
                                    " from the bill count", "");
                        }
                        if (bill.getAllowedToExceedLimit().equals(Boolean.TRUE)) {
                            //split the amount
                            PatientBillItem b = bill.getBillItems().get(0);
                            Double sellingPrice = b.getAmount();
                            Double runningLimit = payDetails.getRunningLimit();
                            Double surpassedAmount = sellingPrice - runningLimit;
                            Double newAmountToBill = surpassedAmount;
                            //create default credit bill
                            if (runningLimit >= 0) {
                                bill.getBillItems().get(0).setAmount(runningLimit);
                                bill.getBillItems().get(0).setBalance(runningLimit);
                                bill.getBillItems().get(0).setBillPayMode(PaymentMethod.Insurance);
                                bill.getBillItems().get(0).setPrice(runningLimit);
                                bill.getBillItems().get(0).setQuantity(1.0);
                            }
                            if (runningLimit < 0) {
                                bill.getBillItems().get(0).setAmount(sellingPrice);
                                bill.getBillItems().get(0).setBalance(sellingPrice);
                                bill.getBillItems().get(0).setBillPayMode(PaymentMethod.Cash);
                                bill.getBillItems().get(0).setPrice(sellingPrice);
                                bill.getBillItems().get(0).setQuantity(1.0);
                            }

                            if (bill.getSurpassedAmountPaymentMethod().equals(ExcessAmountPayMethod.Cash)) {
                                //create cash bill for the surpassed amount
                                PatientBillItem nb = PatientBillItem.clone(bill.getBillItems().get(0));
                                nb.setAmount(runningLimit >= 0 ? surpassedAmount : sellingPrice);
                                nb.setBalance(runningLimit >= 0 ? surpassedAmount : sellingPrice);

                                nb.setPrice(runningLimit >= 0 ? surpassedAmount : sellingPrice);
                                nb.setQuantity(1.0);

                                nb.setBillPayMode(PaymentMethod.Cash);
                                bill.getBillItems().add(nb);
                            }

                            if (bill.getSurpassedAmountPaymentMethod().equals(ExcessAmountPayMethod.Discount)) {
                                //write off surpassed amount
                                throw APIException.internalError("Some parameters are not set");//for managing end user
                            }
                        }
                    }

                    if (payDetails.getRunningLimit() < amountToBill && payDetails.getExcessAmountEnabled()) {
                        //check if
                        if (payDetails.getLimitReached()) {
                            //proceed to accept excess entry
                            //TODO: register to keep log of the excess amounts with correct pricebook
                            for (PatientBillItem b : bill.getBillItems()) {
                                BigDecimal defaultPrice = b.getItem().getRate(); //default cash selling price
                                if (payDetails.getExcessAmountPayMode() == PaymentMethod.Cash) {
                                    b.setAmount(NumberUtils.createDouble(defaultPrice.toString()));
                                    b.setBillPayMode(PaymentMethod.Cash);
                                } else {
                                    try {
                                        pricelistService.fetchPriceAmountByItemAndPriceBook(b.getItem(), payDetails.getExcessAmountPayer().getPriceBook());
                                        b.setBillPayMode(PaymentMethod.Insurance);
                                        b.setScheme(payDetails.getExcessAmountScheme());
                                    } catch (Exception e) {
                                        b.setAmount(NumberUtils.createDouble(defaultPrice.toString()));
                                    }
                                }
                            }
                        }
                        if (!payDetails.getLimitReached() && itemCount > 0) {
                            throw APIException.badRequest("Bill amount (" + amountToBill + ") exceed \nrunning limit amount (" + payDetails.getRunningLimit() + "). \nRemove one or more items from the bill count", "");
                        }

                    }
//                    }
                }
            }
        }

        log.debug("END validate limit amount");

        String bill_no = bill.getBillNumber() == null ? sequenceNumberService.next(1L, Sequences.BillNumber.name()) : bill.getBillNumber();
        String trdId = bill.getTransactionId() == null ? sequenceNumberService.next(1L, Sequences.Transactions.name()) : bill.getTransactionId();
        bill.setBillNumber(bill_no);
        bill.setTransactionId(trdId);
//        //This code caters for inpatient apparently, with no much clarifications
//        //TODO: validate if inpatient has any deposits or pre-payments - can be globally set
//        if (bill.getVisit() != null ) {
//            System.out.println("TO create bill visit type "+bill.getVisit().getVisitType().name());
//            if(bill.getVisit().getVisitType().equals(VisitEnum.VisitType.Inpatient)){
//                bill.setStatus(BillStatus.Paid);
//            }
//        }
        for (PatientBillItem bi : bill.getBillItems()) {
            System.out.println(bi.getItem().getItemName() + " Amount: " + bi.getAmount() + " Paymode " + bi.getBillPayMode().name());
        }

        PatientBill savedBill = patientBillRepository.saveAndFlush(bill);

        if (bill.getWalkinFlag() == null || bill.getWalkinFlag()) {
            return savedBill;
        }
        List<DoctorInvoice> doctorInvoices = toDoctorInvoice(savedBill);
        if (doctorInvoices.size() > 0) {
            doctorInvoices.forEach(inv -> doctorInvoiceService.save(inv));
        }
        //reduce limit amount
        if (pd != null && pd.isPresent()) {
            PaymentDetails pdd = pd.get();
            double newRunningLimit = (pdd.getRunningLimit() - amountToBill);
            pdd.setRunningLimit(newRunningLimit);
            pdd.setTempRunningLimit(newRunningLimit);

            if (newRunningLimit <= amountToBill) {
                pdd.setLimitReached(Boolean.TRUE);
            }
            paymentDetailsService.createPaymentDetails(pdd);
        }

        return savedBill;
    }

    public PatientBill update(PatientBill bill) {
        return patientBillRepository.save(bill);
    }

    public Optional<PatientBill> findByBillNumber(final String billNumber) {
        return patientBillRepository.findByBillNumber(billNumber);
    }

    public List<PatientBill> findByVisit(final String visitNumber) {
        Optional<Visit> visit = visitRepository.findByVisitNumber(visitNumber);
        if (visit.isPresent()) {
            return patientBillRepository.findByVisit(visit.get());
        } else {
            return null;
        }
    }

    public PatientBill findOneWithNoFoundDetection(Long id) {
        return patientBillRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("Bill with Id {0} not found", id));
    }

    public PatientBillItem findBillItemById(Long id) {
        return billItemRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("Bill Item with Id {0} not found", id));
    }

    public PatientBillItem findBillItemByPatientBillAndItem(final PatientBill patientBill, final Item item) {
        return billItemRepository.findByPatientBillAndItem(patientBill, item)
                .orElseThrow(() -> APIException.notFound("Bill item not found"));
    }

    public PatientBillItem findBillItemByPatientBill(String billNumber) {
        Optional<PatientBill> patientBill = findByBillNumber(billNumber);
//        if (patientBill.isPresent()) {
//            Optional<PatientBillItem> billItem = billItemRepository.findByPatientBill(patientBill.get());
//            if (billItem.isPresent()) {
//                return billItem.get();
//            } else {
//                return null;
//            }
//        }
        return null;
    }

    @Transactional
    public void cancelBillItem(List<PatientBillItem> billItems) {
        billItems.stream()
                .map(x -> {
                    x.setStatus(BillStatus.Canceled);
                    return x;
                })
                .forEach(bill -> billItemRepository.save(bill));
        //TODO be able to reverse the doctors fee
    }

    public void cancelItem(Long billId) {

    }

    public PatientBillItem updateBillItem(PatientBillItem item) {
        //determine the request origin and update ti
        cashPaidUpdater.updateRequestStatus(item);
        //update the doctors payments with this receipts
//       TODO
        return billItemRepository.save(item);
    }

    public Item getItemByCode(String code) {
        return itemService.findByItemCodeOrThrow(code);
    }

    public Item getItemByBy(Long id) {
        return itemService.findById(id)
                .orElseThrow(() -> APIException.notFound("Service with Item Id {0} Not Found", id));
    }

    public String addPatientBillItems(Long id, List<BillItemData> billItems) {
        PatientBill patientbill = findOneWithNoFoundDetection(id);
        List<PatientBillItem> lineItems = billItems
                .stream()
                .map(lineData -> {
                    Item item = itemService.findByItemCodeOrThrow(lineData.getItemCode());
                    PatientBillItem billLine = new PatientBillItem();

                    billLine.setBillingDate(lineData.getBillingDate());
                    billLine.setTransactionId(lineData.getTransactionId());
                    billLine.setItem(item);
                    billLine.setPrice(lineData.getPrice());
                    billLine.setQuantity(lineData.getQuantity());
                    billLine.setAmount(lineData.getAmount());
                    billLine.setDiscount(lineData.getDiscount());
                    billLine.setBalance(lineData.getAmount());
                    billLine.setServicePoint(lineData.getServicePoint());
                    billLine.setServicePointId(lineData.getServicePointId());
                    billLine.setStatus(BillStatus.Draft);
                    billLine.setBillPayMode(lineData.getPaymentMethod());
                    billLine.setEntryType(BillEntryType.Debit);

                    return billLine;
                })
                .collect(Collectors.toList());
        patientbill.addBillItems(lineItems);

        return patientbill.getBillNumber();
    }

    @Deprecated
    public Page<PatientBill> findAllBills(String transactionNo, String visitNo, String patientNo, String paymentMode, String billNo, BillStatus status, DateRange range, Pageable page) {

        Specification<PatientBill> spec = BillingSpecification.createSpecification(transactionNo, visitNo, patientNo, paymentMode, billNo, status, range);

        return patientBillRepository.findAll(spec, page);

    }

    public Page<PatientBillItem> getPatientBillItems(String patientNo, String visitNo, String billNumber, String transactionId, Long servicePointId, Boolean hasBalance, BillStatus status, DateRange range, Pageable page) {
        Specification<PatientBillItem> spec = PatientBillSpecification.createSpecification(patientNo, visitNo, billNumber, transactionId, servicePointId, hasBalance, status, range);

        Page<PatientBillItem> lists = billItemRepository.findAll(spec, page);

        return lists;
    }

    public List<PatientBillItem> getPatientBillItem(String transactionNo) {
        return billItemRepository.findByTransactionId(transactionNo);
    }

//    public Page<SummaryBill> getSummaryBill(String visitNumber, String patientNumber, Boolean hasBalance, DateRange range, Pageable pageable) {
//        return billItemRepository.getBillSummary(visitNumber, patientNumber, hasBalance, range, pageable);
//    }
//
//    public Page<SummaryBill> getWalkinSummaryBill(String patientNumber, Boolean hasBalance, Pageable pageable) {
//        return billItemRepository.getWalkinBillSummary(patientNumber, hasBalance, pageable);
//    }

    public Page<PatientBillItem> getReceiptedBillItems(String visitNo, Pageable page) {
        Visit visit = findVisitEntityOrThrow(visitNo);
        Specification<PatientBillItem> spec = PatientBillSpecification.getReceiptedItems(visitNo, visit.getPaymentMethod());

        Page<PatientBillItem> lists = billItemRepository.findAll(spec, page);

        return lists;
    }

    public Page<PatientBillItem> getWalkBillItems(String walkIn, Boolean hasBalance, Pageable page) {
        Specification<PatientBillItem> spec = PatientBillSpecification.getWalkinBillItems(walkIn, hasBalance);
        return billItemRepository.findAll(spec, page);
    }

    public Visit findVisitEntityOrThrow(String visitNumber) {
        return this.visitRepository.findByVisitNumber(visitNumber)
                .orElseThrow(() -> APIException.notFound("Visit Number {0} not found.", visitNumber));
    }

    public List<PatientBillGroup> getPatientBillGroups(BillStatus status) {
        return patientBillRepository.groupBy(status);
    }

    //    @Deprecated
//    public Page<PatientBillItem> getPatientBillItemByVisit(String visitNumber, Pageable page) {
//        Visit visit = findVisitEntityOrThrow(visitNumber);
//
//        return billItemRepository.findPatientBillItemByVisit(visit, page);
//    }
//    @Deprecated
//    public List<BillData> withBalances() {
//        return patientBillRepository.findAll(billHasBalance()).stream().map(x -> x.toData()).collect(Collectors.toList());
//    }
//    @Deprecated
//    private Specification<PatientBill> billHasBalance() {
//        return (Root<PatientBill> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
//            return cb.greaterThan(root.get("balance"), 0);
//        };
//    }
    private JournalEntry toJournal(PatientBill bill, Store store) {
        String description = "Patient Billing";
        if (bill.getPatient() != null) {
            Patient patient = bill.getPatient();
            description = patient.getPatientNumber() + " " + patient.getFullName();
        }

        Optional<FinancialActivityAccount> debitAccount = activityAccountRepository.findByFinancialActivity(FinancialActivity.Patient_Control);
        if (!debitAccount.isPresent()) {
            throw APIException.badRequest("Patient Control Account is Not Mapped");
        }
//        String debitAcc = debitAccount.get().getAccount().getIdentifier();
        List<JournalEntryItem> items = new ArrayList<>();

        if (!bill.getBillItems().isEmpty()) {
            Map<Long, Double> map = bill.getBillItems()
                    .stream()
                    .collect(Collectors.groupingBy(PatientBillItem::getServicePointId,
                            Collectors.summingDouble(PatientBillItem::getAmount)
                            )
                    );
            //then here since we making a revenue
            map.forEach((k, v) -> {

                ServicePoint srv = servicePointService.getServicePoint(k);
                String desc = srv.getName() + " Patient Billing";
                Account credit = srv.getIncomeAccount();
                BigDecimal amount = BigDecimal.valueOf(v);

                items.add(new JournalEntryItem(debitAccount.get().getAccount(), desc, amount, BigDecimal.ZERO));
                items.add(new JournalEntryItem(credit, desc, BigDecimal.ZERO, amount));

            });
            //if inventory expenses this shit!
            if (store != null) {
                Map<Long, Double> inventory = bill.getBillItems()
                        .stream()
                        .filter(x -> x.getItem().isInventoryItem())
                        .collect(Collectors.groupingBy(PatientBillItem::getServicePointId,
                                Collectors.summingDouble(x -> (x.getItem().getCostRate().doubleValue() * x.getQuantity()))
                                )
                        );
                if (!inventory.isEmpty()) {
                    inventory.forEach((k, v) -> {
                        //revenue
                        String pat = "";
                        if (bill.getPatient() != null) {
                            pat = bill.getPatient().getPatientNumber() + " - " + bill.getPatient().getFullName();
                        }
                        //TODO
                        String desc = "Issuing Stocks to " + pat;
                        ServicePoint srv = servicePointService.getServicePoint(k);
                        Account debit = srv.getExpenseAccount(); // cost of sales
                        Account credit = srv.getInventoryAssetAccount();//store.getInventoryAccount(); // Inventory Asset Account
                        BigDecimal amount = BigDecimal.valueOf(v);

                        items.add(new JournalEntryItem(debit, desc, amount, BigDecimal.ZERO));
                        items.add(new JournalEntryItem(credit, desc, BigDecimal.ZERO, amount));

                    });
                }
            }
        }

        JournalEntry toSave = new JournalEntry(bill.getBillingDate(), description, items);
        toSave.setTransactionNo(bill.getTransactionId());
        toSave.setTransactionType(TransactionType.Billing);
        toSave.setStatus(JournalState.PENDING);
        return toSave;
    }

    private List<DoctorInvoice> toDoctorInvoice(PatientBill bill) {
        return bill.getBillItems()
                .stream()
                .map(billItem -> {
                    if (billItem.getMedicId() == null) {
                        return null;
                    }
                    Employee doctor = doctorInvoiceService.getDoctorById(billItem.getMedicId());
                    Optional<DoctorItem> doctorItem = doctorInvoiceService.getDoctorItem(doctor, billItem.getItem());

                    if (doctorItem.isPresent()) {
                        DoctorItem docItem = doctorItem.get();
                        if (docItem.getActive()) {
                            BigDecimal amt = computeDoctorFee(docItem);
                            DoctorInvoice invoice = new DoctorInvoice();
                            invoice.setAmount(amt);
                            invoice.setBalance(amt);
                            invoice.setDoctor(doctor);
                            invoice.setInvoiceDate(billItem.getBillingDate());
                            invoice.setInvoiceNumber(billItem.getPatientBill().getBillNumber());
                            invoice.setBillItemId(billItem.getId());
                            invoice.setPaid(Boolean.FALSE);
                            invoice.setPatient(billItem.getPatientBill().getPatient());
                            invoice.setPaymentMode(billItem.getPatientBill().getPaymentMode());
                            invoice.setServiceItem(docItem);
                            invoice.setTransactionId(billItem.getTransactionId());
                            invoice.setVisit(billItem.getPatientBill().getVisit());
                            return invoice;
                        }
                    }
                    return null;
                })
                .filter(x -> x != null)
                .collect(Collectors.toList());
    }

    private BigDecimal computeDoctorFee(DoctorItem item) {
        if (item.getIsPercentage()) {
            BigDecimal doctorRate = item.getAmount().divide(BigDecimal.valueOf(100)).multiply(item.getServiceType().getRate());
            return doctorRate;
        }
        return item.getAmount();
    }

    //    public List<PatientBill> search(String search) {
//        BillSpecificationsBuilder builder = new BillSpecificationsBuilder();
//
//        String operationSetExper = StringUtils.join(SearchOperation.SIMPLE_OPERATION_SET, "|");
//        Pattern pattern = Pattern.compile("(\\w+?)(" + operationSetExper + ")(\\p{Punct}?)(\\w+?)(\\p{Punct}?),");
//        Matcher matcher = pattern.matcher(search + ",");
//        while (matcher.find()) {
//            builder.with(matcher.group(1), matcher.group(2), matcher.group(4), matcher.group(3), matcher.group(5));
//        }
//
//        Specification<PatientBill> spec = builder.build();
//        return patientBillRepository.findAll(spec);
//    }
    @Transactional
    public List<PatientBillItem> validatedBilledItem(ReceivePayment data) {

        List<PatientBillItem> receiptingBills = new ArrayList<>();
        List<PatientBillItem> toCreate = new ArrayList<>();

        if (!data.getBillItems().isEmpty()) {
            data.getBillItems().stream()
                    .forEach(x -> {
                        if (x.getBillItemId() != null) {
                            PatientBillItem item = findBillItemById(x.getBillItemId());
                            BigDecimal discount = (x.getDiscount() != null ? x.getDiscount() : BigDecimal.ZERO);
//                            BigDecimal subtotal = item.getNetAmount() != null && item.getNetAmount() > 0 ? BigDecimal.valueOf(item.getNetAmount()) : BigDecimal.valueOf(((item.getQuantity() * item.getPrice()) - discount.doubleValue()));

                            BigDecimal totalAmount = BigDecimal.valueOf((item.getQuantity() * item.getPrice()));
                            BigDecimal netAmount = totalAmount.subtract(discount);
                            BigDecimal balance = netAmount.subtract(x.getAmount());

//                            BigDecimal amount = subtotal; // + item.getTaxes();
//                            BigDecimal amount = BigDecimal.valueOf(((item.getQuantity() * item.getPrice()) - discount.doubleValue()));
//                            BigDecimal bal = amount.subtract(x.getAmount());  //what was paid less the discount
                            item.setPaid(Boolean.TRUE);
                            item.setStatus(BillStatus.Paid);
                            if (item.getItem().getCategory() == ItemCategory.CoPay) {
//                                item.setAmount((item.getAmount() * -1));
                                item.setEntryType(BillEntryType.Credit);
                            } else {
                                item.setAmount(totalAmount.doubleValue());
                            }
//                            item.setSubTotal(subtotal.doubleValue());
                            item.setPaymentReference(data.getReceiptNo());
                            item.setBalance(balance.doubleValue());

                            item.setDiscount(discount.doubleValue());

                            PatientBillItem i = updateBillItem(item);

                            receiptingBills.add(i);

                        } else {
                            PatientBillItem item = createReciptItem(x);
                            if (item.getItem().getCategory() == ItemCategory.CoPay) {
                                item.setAmount((item.getAmount() * -1));
                                item.setEntryType(BillEntryType.Credit);
                            }
                            item.setMedicId(x.getMedicId());
                            item.setTransactionId(data.getTransactionNo());
                            item.setPaymentReference(data.getReceiptNo());
                            toCreate.add(item);
                        }
                    });
        }
        if (!toCreate.isEmpty()) {
            PatientBill patientbill = new PatientBill();
            if (data.getWalkin() != null && !data.getWalkin()) {
                Visit visit = findVisitEntityOrThrow(data.getVisitNumber());
                patientbill.setVisit(visit);
                patientbill.setPatient(visit.getPatient());
                patientbill.setWalkinFlag(Boolean.FALSE);
            } else {
                patientbill.setReference(data.getPayerNumber());
                patientbill.setOtherDetails(data.getPayer());
                patientbill.setWalkinFlag(Boolean.TRUE);
            }
            Double amount = toCreate.stream()
                    .collect(Collectors.summingDouble(x -> x.getAmount()));

            patientbill.setAmount(amount);
            patientbill.setDiscount(0D);
            patientbill.setBalance(0D);

            patientbill.setBillingDate(LocalDate.now());
            patientbill.setPaymentMode("Cash");

            patientbill.setStatus(BillStatus.Paid);

            String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

            patientbill.setBillNumber(bill_no);
            patientbill.setTransactionId(data.getTransactionNo());
            patientbill.addBillItems(toCreate);

            PatientBill savedBill = save(patientbill);

            receiptingBills.addAll(savedBill.getBillItems());

        }

        //insert an entry to represent the receipted bill here
        //Create a receipt entry
        return receiptingBills;
    }

    private PatientBillItem createReciptItem(BillReceiptedItem billReceiptedItem) {
        Item item = getItemByBy(billReceiptedItem.getPricelistItemId());

        PatientBillItem savedItem = new PatientBillItem();
        savedItem.setPrice(billReceiptedItem.getPrice());
        savedItem.setQuantity(billReceiptedItem.getQuantity());
        savedItem.setAmount(billReceiptedItem.getAmount().doubleValue());
        savedItem.setBalance(billReceiptedItem.getAmount().doubleValue());
        savedItem.setBillingDate(LocalDate.now());
        savedItem.setDiscount(0D);
        savedItem.setTaxes(0D);
        savedItem.setItem(item);
        savedItem.setPaid(Boolean.TRUE);
        savedItem.setStatus(BillStatus.Paid);
        savedItem.setBalance(0D);
        savedItem.setServicePoint(billReceiptedItem.getServicePoint());
        savedItem.setServicePointId(billReceiptedItem.getServicePointId());
        savedItem.setBillPayMode(PaymentMethod.Cash);
        savedItem.setEntryType(BillEntryType.Credit);

        return savedItem;
    }
    //TODO 2020-05-03 -> filter all the bills and group them together

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PatientBill createCopay(CopayData data) {
        String visitNumber = data.getVisitNumber();
        Long schemeId = data.getSchemeId();
        //TODO check if the visit is valid or not
        Optional<Visit> visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.CheckIn);
        if (!visitOptional.isPresent()) {
            visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.Admitted);
            if (!visitOptional.isPresent()) {
                visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.Discharged);
            }
        }
        if (!visitOptional.isPresent()) {
            throw APIException.notFound("No active visit found for the patient selected");
        }

        Visit visit = visitOptional.get();

        Scheme scheme = schemeService.fetchSchemeById(schemeId);

        Optional<SchemeConfigurations> schemeConfigs = schemeService.fetchSchemeConfigByScheme(scheme);
        if (schemeConfigs.isPresent()) {
            SchemeConfigurations config = schemeConfigs.get();
            BigDecimal copayAmount = BigDecimal.valueOf(config.getCoPayValue());

            if (config.getCoPayType() == CoPayType.Percentage) {
                if (data.getVisitStart()) {
                    return null;
                }

                BigDecimal currentBill = billItemRepository.getTotalBill(visitNumber);
                copayAmount = (copayAmount.divide(BigDecimal.valueOf(100))).multiply(currentBill);
            }
            if (copayAmount != null && copayAmount != BigDecimal.ZERO) {
                //create the bill
                Double copay = copayAmount.doubleValue();
                if (copay <= 0) {
                    return null;
                }
                Optional<Item> copayItem = itemService.findFirstByCategory(ItemCategory.CoPay);
                if (copayItem.isPresent()) {
                    Item item = copayItem.get();
                    PatientBill patientbill = new PatientBill();
                    patientbill.setVisit(visit);
                    patientbill.setPatient(visit.getPatient());
                    patientbill.setWalkinFlag(Boolean.FALSE);
                    patientbill.setAmount(copay);
                    patientbill.setDiscount(0D);
                    patientbill.setBalance(copay);
                    patientbill.setBillingDate(LocalDate.now());
                    patientbill.setPaymentMode(visit.getPaymentMethod().name());
                    patientbill.setStatus(BillStatus.Draft);

                    String trdId = sequenceNumberService.next(1L, Sequences.Transactions.name());
                    String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

                    patientbill.setBillNumber(bill_no);
                    patientbill.setTransactionId(trdId);

                    PatientBillItem billItem = new PatientBillItem();

                    billItem.setBillingDate(LocalDate.now());
                    billItem.setTransactionId(trdId);
                    billItem.setItem(item);
                    billItem.setPaid(false);
                    billItem.setPrice(copay);
                    billItem.setQuantity(1D);
                    billItem.setAmount(copay);
                    billItem.setDiscount(0D);
                    billItem.setBalance(copay);
                    //determine where the copay should be posted
                    billItem.setServicePoint("Copayment");
                    billItem.setServicePointId(0L);
                    billItem.setStatus(BillStatus.Draft);
                    billItem.setMedicId(null);
                    billItem.setBillPayMode(PaymentMethod.Cash);
                    billItem.setEntryType(BillEntryType.Credit);

                    patientbill.addBillItem(billItem);

                    return save(patientbill);
                }

            }
        }
        return null;
    }

    // Get Bills
    public List<SummaryBill> getBillTotals(String visitNumber, String patientNumber, Boolean hasBalance, Boolean isWalkin, PaymentMethod paymentMode, DateRange range, Boolean includeCanceled, VisitEnum.VisitType visitType) {
        return billItemRepository.getBillSummary(visitNumber, patientNumber, hasBalance, isWalkin, paymentMode, range, includeCanceled, visitType);
    }

    public BillDetail getBillDetails(String visitNumber, boolean includeCanceled, PaymentMethod paymentMethod, Pageable pageable) {
        List<PatientBillItem> patientItems = billItemRepository.findAll(withVisitNumber(visitNumber, includeCanceled, paymentMethod, null));
        List<PatientBillItem> walkinItems = billItemRepository.findAll(withWalkinNumber(visitNumber, includeCanceled, paymentMethod));
        if (!walkinItems.isEmpty()) {
            patientItems.addAll(walkinItems);
        }
        BillDetail details = new BillDetail();

        List<BillItem> bills = new ArrayList<>();
        List<BillItem> paidBills = new ArrayList<>();
        List<BillPayment> payments = new ArrayList<>();

        patientItems.stream()
                .forEach(x -> {
                    if (x.getBalance() > 0 && (x.getStatus() == BillStatus.Draft)) {
                        bills.add(x.toBillItem());
                    } else if (x.getStatus() == BillStatus.Paid && x.isFinalized() == false) {
                        if (x.getItem().getCategory() != ItemCategory.Receipt) {
                            paidBills.add(x.toBillItem());
                        }
                        //only return receipts
                        if (x.getItem().getCategory() == ItemCategory.CoPay || x.getItem().getCategory() == ItemCategory.Receipt || x.getItem().getCategory() == ItemCategory.NHIF_Rebate) {
                            BillPayment.Type type = x.getItem().getCategory() == ItemCategory.CoPay ? BillPayment.Type.Copayment : BillPayment.Type.Receipt;
                            ReceiptType receiptType = ReceiptType.Payment;

                            if (x.getPaymentReference() != null) {
                                Optional<Receipt> receipt = receiptRepository.findByReceiptNo(x.getPaymentReference());
                                if (receipt.isPresent()) {
                                    receiptType = receipt.get().toData().getReceiptType();
                                }
                            }

                            BigDecimal amount = BigDecimal.valueOf(x.getAmount());
                            if (amount.signum() == -1) {
                                amount = amount.negate();
                            }
                            payments.add(new BillPayment(type, x.getPaymentReference(), amount, receiptType));
                        }
                    } else {
                        System.err.println("Canceled Billed");
                    }
                });

        details.setBills(bills);
        details.setPaidBills(paidBills);
        details.setPayments(
                payments.stream()
                        .filter(distinctByKey(BillPayment::getReference))
                        .collect(Collectors.toList())
        );

        return details;

//        Map<BillPayment.Type, Double> paymentlist = allPayments
//                .stream()
//                .collect(
//                        Collectors.groupingBy(BillPayment::getType, Collectors.summingDouble(BillPayment::getAmount))
//                );
//          List<BillPayment> payments = new ArrayList<>();
//        paymentlist
//                .forEach((k, v) -> {
//                     payments.add(new BillPayment(k, visitNumber, v));
//                });
    }
    //when changing billing I should only show those

    public List<BillItem> getAllBillDetails(String visitNumber, boolean includeCanceled) {
        List<PatientBillItem> patientItems = billItemRepository.findAll(withVisitNumber(visitNumber, includeCanceled, null, null));
        List<PatientBillItem> walkinItems = billItemRepository.findAll(withWalkinNumber(visitNumber, includeCanceled, null));
        if (!walkinItems.isEmpty()) {
            patientItems.addAll(walkinItems);
        }
        BillDetail details = new BillDetail();

        List<BillItem> bills = new ArrayList<>();

        patientItems.stream()
                .forEach(x -> {
                    bills.add(x.toBillItem());
                });

        details.setBills(bills);
        return bills;
    }

    private Specification<PatientBillItem> withVisitNumber(String visitNo, boolean includeCanceled, PaymentMethod paymentMethod, BillEntryType billEntryType) {
        return (Root<PatientBillItem> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("patientBill").get("walkinFlag"), Boolean.FALSE));
            if (visitNo != null) {
                predicates.add(cb.equal(root.get("patientBill").get("visit").get("visitNumber"), visitNo));
            }
            if (!includeCanceled) {
                predicates.add(cb.notEqual(root.get("status"), BillStatus.Canceled));
            }
            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("billPayMode"), paymentMethod));
            }

            if (billEntryType != null) {
                predicates.add(cb.equal(root.get("entryType"), billEntryType));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    private Specification<PatientBillItem> withWalkinNumber(String walkIn, boolean includeCanceled, PaymentMethod paymentMethod) {
        return (Root<PatientBillItem> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("patientBill").get("walkinFlag"), Boolean.TRUE));

            if (!includeCanceled) {
                predicates.add(cb.notEqual(root.get("status"), BillStatus.Canceled));
            }

            if (walkIn != null) {
                predicates.add(cb.equal(root.get("patientBill").get("reference"), walkIn));
            }
            if (paymentMethod != null) {

                predicates.add(cb.equal(root.get("billPayMode"), paymentMethod));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    private JournalEntry toReverseJournal(List<PatientBillItem> bills, Store store) {

        System.err.println("Reversing the journal for the patient bills");
        String trdId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        String description = "Patient Billing Reversing";
        Optional<FinancialActivityAccount> reverseCredit = activityAccountRepository.findByFinancialActivity(FinancialActivity.Patient_Control);
        if (!reverseCredit.isPresent()) {
            throw APIException.badRequest("Patient Control Account is Not Mapped");
        }
//        String debitAcc = debitAccount.get().getAccount().getIdentifier();
        List<JournalEntryItem> items = new ArrayList<>();

        if (!bills.isEmpty()) {
            Map<Long, Double> map = bills
                    .stream()
                    .collect(Collectors.groupingBy(PatientBillItem::getServicePointId,
                            Collectors.summingDouble(PatientBillItem::getAmount)
                            )
                    );
            //then here since we making a revenue
            map.forEach((k, v) -> {

                ServicePoint srv = servicePointService.getServicePoint(k);
                String desc = srv.getName() + " Patient Billing Reversal";
                Account reverseDebit = srv.getIncomeAccount();
                BigDecimal amount = BigDecimal.valueOf(v);

                items.add(new JournalEntryItem(reverseDebit, desc, amount, BigDecimal.ZERO));
                items.add(new JournalEntryItem(reverseCredit.get().getAccount(), desc, BigDecimal.ZERO, amount));

            });
            //if inventory expenses this shit!
            if (store != null) {
                Map<Long, Double> inventory = bills
                        .stream()
                        .filter(x -> x.getItem().isInventoryItem())
                        .collect(Collectors.groupingBy(PatientBillItem::getServicePointId,
                                Collectors.summingDouble(x -> (x.getItem().getCostRate().doubleValue() * x.getQuantity()))
                                )
                        );
                if (!inventory.isEmpty()) {
                    inventory.forEach((k, v) -> {

                        String desc = "Stock Returns ";
                        ServicePoint srv = servicePointService.getServicePoint(k);
                        Account creditExpense = srv.getExpenseAccount(); // cost of sales
                        Account debitInventory = srv.getInventoryAssetAccount();//store.getInventoryAccount(); // Inventory Asset Account
                        BigDecimal amount = BigDecimal.valueOf(v);

                        items.add(new JournalEntryItem(debitInventory, desc, amount, BigDecimal.ZERO));
                        items.add(new JournalEntryItem(creditExpense, desc, BigDecimal.ZERO, amount));

                        //this should return the stocks
                    });
                }
            }
        }

        JournalEntry toSave = new JournalEntry(LocalDate.now(), description, items);
        toSave.setTransactionNo(trdId);
        toSave.setTransactionType(TransactionType.Bill_Reversal);
        toSave.setStatus(JournalState.PENDING);
        return toSave;
    }

    //TODO - cancelling of a bill item
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<PatientBillItem> voidBillItem(String visitNumber, List<VoidBillItem> items) {

        List<PatientBillItem> toVoidList = items
                .stream()
                .map(x -> billItemRepository.findById(x.getBillItemId()).orElse(null))
                .filter(bill -> bill != null)
                .map(patientBill -> {
                    patientBill.setStatus(BillStatus.Canceled);
                    patientBill.setBalance(0D);
                    patientBill.setPaid(Boolean.FALSE);
                    return patientBill;
                })
                .collect(Collectors.toList());

        List<PatientBillItem> bills = billItemRepository.saveAll(toVoidList);

        if (!bills.isEmpty()) {
            PatientBill pb = bills.get(0).getPatientBill();

            if (pb.getPaymentMode().equals("Insurance")) {
                journalService.save(toReverseJournal(bills, null));
            }
        }
        return bills;
    }

    @Transactional
    public List<PatientBillItem> updatePatientBills(String visitNumber, List<BillItemData> billItems) {
        List<PatientBillItem> currentBills = billItemRepository.getByVisitNumber(visitNumber);
        currentBills.stream()
                .filter(x -> x.getEntryType() == BillEntryType.Debit)
                .map(x -> {
                    x.setStatus(BillStatus.Canceled);
                    return x;
                }).forEach(b -> billItemRepository.save(b));

        String trdId = sequenceNumberService.next(1L, Sequences.Transactions.name());

        List<PatientBillItem> items = billItems.stream()
                .map(x -> {
                    PatientBillItem item = billItemRepository.findById(x.getId()).get();
                    if (item.getItem().getItemType() == ItemType.Inventory && item.getQuantity() != x.getQuantity()) {
                        //1 - 2 = 1
                        ServicePoint servicePoint = servicePointService.getServicePoint(item.getServicePointId());
                        double qty = (x.getQuantity() - item.getQuantity());
                          if(qty > 0 ){ // an increase in qty
                               updateStockItem(item, servicePoint.getStore(), qty, trdId);
                            }else if (qty < 0){
                                qty *= -1;
                                updateStockItem(item, qty, trdId);
                            }
                      }
                      item.setPrice(x.getPrice());
                    item.setQuantity(x.getQuantity());
                    item.setStatus(x.getStatus());
                    item.setAmount(x.getAmount());
                    item.setBalance(x.getAmount());

                    return item;
                })
                .collect(Collectors.toList());

        return billItemRepository.saveAll(items);

    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PatientBill createFee(String admissionNumber, ItemCategory category, Integer qty) {
        Double sellingRate = 0.0;
        //TODO check if the visit is valid or not
        Visit visit = visitRepository.findByVisitNumberAndStatusNot(admissionNumber, VisitEnum.Status.CheckOut)
                .orElseThrow(() -> APIException.badRequest("Admission Number {0} is not active for transaction", admissionNumber));
        PatientBill patientbill = null;
        PriceList priceList = pricelistService.fetchPriceListByItemCategory(category);
        if (category == ItemCategory.NHIF_Rebate) {
            sellingRate = 1 * (NumberUtils.toDouble(priceList.getSellingRate()));

        } else {
            sellingRate = (NumberUtils.toDouble(priceList.getSellingRate()));
        }
        patientbill = new PatientBill();
        patientbill.setVisit(visit);
        patientbill.setPatient(visit.getPatient());
        patientbill.setWalkinFlag(Boolean.FALSE);
        patientbill.setAmount(sellingRate * qty);
        patientbill.setDiscount(0D);
        patientbill.setBalance(sellingRate);
        patientbill.setBillingDate(LocalDate.now());
        patientbill.setPaymentMode(visit.getPaymentMethod().name());
        patientbill.setStatus(BillStatus.Paid); //27944

        String trdId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

        patientbill.setBillNumber(bill_no);
        patientbill.setTransactionId(trdId);

        PatientBillItem billItem = new PatientBillItem();

        billItem.setBillingDate(LocalDate.now());
        billItem.setTransactionId(trdId);
        billItem.setItem(priceList.getItem());
        billItem.setPrice(sellingRate);
        billItem.setQuantity(NumberUtils.createDouble(String.valueOf(qty)));
        billItem.setAmount(sellingRate * qty);
        billItem.setDiscount(0D);
        billItem.setBalance(sellingRate * qty);
        billItem.setServicePoint(priceList.getServicePoint().getName());
        billItem.setServicePointId(priceList.getServicePoint().getId());

        billItem.setMedicId(null);
        billItem.setBillPayMode(visit.getPaymentMethod());
        if (category == ItemCategory.Admission) {
            billItem.setEntryType(BillEntryType.Debit);
            billItem.setStatus(BillStatus.Draft);
        } else {
            billItem.setEntryType(BillEntryType.Credit);
            billItem.setStatus(BillStatus.Paid);
            billItem.setPaid(true);
        }
        patientbill.addBillItem(billItem);

        return save(patientbill);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PatientBill createReceipt(PatientReceipt data) {
        String visitNumber = data.getVisitNumber();
        BigDecimal amount = data.getReceipt().getAmount().negate();
        Double receiptedAmount = (amount.doubleValue() * -1);

//        Visit visit = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.CheckIn)
//                .orElseThrow(() -> APIException.badRequest("Visit Number {0} is not active for transaction", visitNumber));
        Optional<Visit> visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.CheckIn);
        if (!visitOptional.isPresent()) {
            visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.Admitted);
            if (!visitOptional.isPresent()) {
                visitOptional = visitRepository.findByVisitNumberAndStatus(visitNumber, VisitEnum.Status.Discharged);
            }
        }
        if (!visitOptional.isPresent()) {
            throw APIException.notFound("No active visit found for the patient selected");
        }

        Visit visit = visitOptional.get();

        Optional<Item> receiptItem = itemService.findFirstByCategory(ItemCategory.Receipt);
        if (receiptItem.isPresent()) {
            Item item = receiptItem.get();
            PatientBill patientbill = new PatientBill();
            patientbill.setVisit(visit);
            patientbill.setPatient(visit.getPatient());
            patientbill.setWalkinFlag(Boolean.FALSE);
            patientbill.setAmount(receiptedAmount);
            patientbill.setDiscount(0D);
            patientbill.setBalance(receiptedAmount);
            patientbill.setBillingDate(data.getReceipt().getTransactionDate().toLocalDate());
            patientbill.setPaymentMode(visit.getPaymentMethod().name());
            patientbill.setStatus(BillStatus.Paid);
            patientbill.setPaymentMode("Cash");

            String trdId = data.getReceipt().getTransactionNo() != null ? data.getReceipt().getTransactionNo() : sequenceNumberService.next(1L, Sequences.Transactions.name());
            String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

            patientbill.setBillNumber(bill_no);
            patientbill.setTransactionId(trdId);

            PatientBillItem billItem = new PatientBillItem();

            billItem.setBillingDate(LocalDate.now());
            billItem.setTransactionId(trdId);
            billItem.setItem(item);
            billItem.setPaid(Boolean.FALSE);
            billItem.setPrice(receiptedAmount);
            billItem.setQuantity(1D);
            billItem.setAmount(receiptedAmount);
            billItem.setDiscount(0D);
            billItem.setBalance(receiptedAmount);
            billItem.setPaymentReference(data.getReceipt().getReceiptNo());
            //determine where the copay should be posted
            billItem.setServicePoint("Receipt");
            billItem.setServicePointId(0L);
            billItem.setStatus(BillStatus.Paid);
            billItem.setMedicId(null);
            billItem.setBillPayMode(PaymentMethod.Cash);
            billItem.setEntryType(BillEntryType.Credit);

            patientbill.addBillItem(billItem);

            return save(patientbill);
        }
        return null;
    }

    public String finalizeBill(String visitNumber, BillFinalizeData finalizeBill) {
        Visit visit = visitRepository.findByVisitNumberAndStatusNot(visitNumber, VisitEnum.Status.CheckOut)
                .orElseThrow(() -> APIException.badRequest("Visit Number {0} is not active for transaction", visitNumber));
        String cinvoice = sequenceNumberService.next(1L, Sequences.CashBillNumber.name());
        List<PatientBillItem> lists = finalizeBill.getBillItems().stream()
                .map(x -> {
                    PatientBillItem item = findBillItemById(x.getBillItemId());
                    if (item.isFinalized() == false) {
                        item.setPaid(Boolean.TRUE);
                        item.setStatus(BillStatus.Paid);
                        if (item.getPaymentReference() == null) {
                            item.setPaymentReference(cinvoice);
                        }
                        item.setFinalized(true);
                        item.setInvoiceNumber(cinvoice);
                        item.setBalance(0D);
                        return item;
                    }
                    return null;
                })
                .filter(x -> x != null)
                .collect(Collectors.toList());

        //TODO consider expensing the deposits received
        List<PatientBillItem> updatedBills = billItemRepository.saveAll(lists);

        updatedBills.forEach(pi -> {
                    if (pi.getItem().getCategory() == ItemCategory.Receipt) {
                        Optional<Receipt> savedReceipt = receiptRepository.findByReceiptNo(pi.getPaymentReference());
                        if (savedReceipt.isPresent() && savedReceipt.get().getPrepayment()) {
                            // we have a deposit to journal it

                        }
                    }
                }
        );

        return cinvoice;
    }

    @Transactional
    public void updatePaymentReference(String oldReference, String newReference) {
        billItemRepository.updatePaymentReference(newReference, oldReference);
    }
//    private JournalEntry toJournalDeposits(Receipt payment, CreateRemittance data) {
//        Optional<FinancialActivityAccount> creditAccount = activityAccountRepository.findByFinancialActivity(FinancialActivity.DeferredRevenue);
//
//        if (!creditAccount.isPresent()) {
//            throw APIException.badRequest("Deferred Revenue (Deposit) Account is Not Mapped for Transaction");
//        }
//        //move the money from here to patient control
//
//        PayChannel channel = data.getPaymentChannel();
//
//        List<JournalEntryItem> items = new ArrayList<>();
//        String descType = "";
//        String liabilityNarration = "Patient Deposit " + payment.getAmount() + " " + (data.getNotes() != null ? "(" + data.getNotes() + ")" : "");
//        items.add(new JournalEntryItem(creditAccount.get().getAccount(), liabilityNarration, BigDecimal.ZERO, payment.getAmount()));
//        //create the invoice payments
//        descType = "Patient Payment deposit";
//
//        //PAYMENT CHANNEL
//        String narration = descType + "  for " + payment.getDescription() + " Reference No : " + payment.getReceiptNo();
//        if (channel.getType() == PayChannel.Type.Bank) {
//            BankAccount bank = bankingService.findBankAccountByNumber(channel.getAccountNumber())
//                    .orElseThrow(() -> APIException.notFound("Bank Account Number {0} Not Found", channel.getAccountNumber()));
//            Account debitAccount = bank.getLedgerAccount();
//            //withdraw this amount from this bank
//            bankingService.deposit(bank, payment, data.getAmount());
//            items.add(new JournalEntryItem(debitAccount, narration, payment.getAmount(), BigDecimal.ZERO));
//            //at this pointwithdraw the cash
//        } else if (channel.getType() == PayChannel.Type.Cash) {
//            Account debitAccount = null;
//            if (channel.getAccountId() == 1) {
//                Optional<FinancialActivityAccount> pettycashAccount = activityAccountRepository.findByFinancialActivity(FinancialActivity.Petty_Cash);
//                if (!pettycashAccount.isPresent()) {
//                    throw APIException.badRequest("Petty Cash Account is Not Mapped");
//                }
//                debitAccount = pettycashAccount.get().getAccount();
//            }
//            if (channel.getAccountId() == 2) {
//                Optional<FinancialActivityAccount> receiptAccount = activityAccountRepository.findByFinancialActivity(FinancialActivity.Receipt_Control);
//                if (!receiptAccount.isPresent()) {
//                    throw APIException.badRequest("Undeposited Fund Account (Receipt Control) is Not Mapped");
//                }
//                debitAccount = receiptAccount.get().getAccount();
//            }
//            if (debitAccount == null) {
//                return null;
//            }
//            items.add(new JournalEntryItem(debitAccount, narration, payment.getAmount(), BigDecimal.ZERO));
//        }
//
//        String description = descType + " Reference No " + payment.getReceiptNo();
//
//        JournalEntry toSave = new JournalEntry(payment.getTransactionDate().toLocalDate(), description, items);
//        toSave.setTransactionType(TransactionType.Receipting);
//        toSave.setTransactionNo(payment.getTransactionNo());
//        toSave.setStatus(JournalState.PENDING);
//        return toSave;
//    }

    //    public List<PatientBillDetail> getPatientBills(String search, String patientNumber, String visitNumber, PaymentMethod paymentMethod, Long payerId, Long schemeId,VisitEnum.VisitType visitType, DateRange range, Pageable pageable){
//        //TODO a better pagination on the creteria search
//        List<PatientBillDetail> patientBills = patientBillRepository.getPatientBills(search, patientNumber, visitNumber, paymentMethod, payerId, schemeId, visitType, range);
//        return patientBills;
//    }
    public Page<PatientBillItem> getPatientBillItems(String visitNumber, boolean includeCanceled, PaymentMethod paymentMethod, BillEntryType billEntryType, Pageable pageable) {
        return billItemRepository.findAll(withVisitNumber(visitNumber, includeCanceled, paymentMethod, billEntryType), pageable);
    }

    private void updateStockItem(PatientBillItem billItem, Store store, Double qty, String trdId) {
        DispensedDrug dispensedDrug = new DispensedDrug();
        Item item = billItem.getItem();

        dispensedDrug.setPatient(billItem.getPatientBill().getPatient());
        dispensedDrug.setDrug(item);
        dispensedDrug.setStore(store);
        dispensedDrug.setDispensedDate(billItem.getBillingDate());
        dispensedDrug.setTransactionId(trdId);
        dispensedDrug.setQtyIssued(qty);
        dispensedDrug.setPrice(billItem.getPrice());
        dispensedDrug.setAmount((billItem.getPrice() * billItem.getQuantity()));
        dispensedDrug.setPaid(billItem.getPaid());
        dispensedDrug.setIsReturn(Boolean.FALSE);
        dispensedDrug.setReturnedQuantity(0D);
        dispensedDrug.setCollected(true);
        dispensedDrug.setDispensedBy(SecurityUtils.getCurrentUserLogin().orElse(""));
        dispensedDrug.setCollectedBy("");
//        dispensedDrug.setInstructions(drugData.getInstructions());
//        dispensedDrug.setOtherReference(drugRequest.getPatientNumber() + " " + drugRequest.getPatientName());
        dispensedDrug.setWalkinFlag(false);
//        dispensedDrug.setBatchNumber(drugData.getBatchNumber());
//        dispensedDrug.setDeliveryNumber(drugData.getBatchNumber());
        dispensedDrug.setVisit(billItem.getPatientBill().getVisit());
//        dispensedDrug.setBillNumber(billItem.getRequestReference());
        //find patient bill item
        dispensedDrug.setBillItem(billItem);

        DispensedDrug savedDrug = dispensedDrugRepository.saveAndFlush(dispensedDrug);
        doStockEntries(savedDrug.getId());
    }


    private void updateStockItem(PatientBillItem item, Double qty, String trdId) {
        StockEntry stock = new StockEntry();

        Optional<DispensedDrug> optionalDrugs = dispensedDrugRepository.findDispensedDrugByBillItem(item);

        if (optionalDrugs.isPresent()) {
            DispensedDrug drugs = optionalDrugs.get();

            DispensedDrug drug1 = ObjectUtils.clone(drugs);

            drug1.setAmount(-1 * (qty) * (drugs.getPrice()));
            drug1.setQtyIssued(-1 * (qty));
            drug1.setCollectedBy(SecurityUtils.getCurrentUserLogin().orElse(""));
            drug1.setIsReturn(Boolean.TRUE);
            drug1.setReturnDate(LocalDate.now());
            drug1.setReturnReason("Bill Adjustments");
            drug1.setId(null);
            drug1.setDispensedBy(SecurityUtils.getCurrentUserLogin().orElse(""));
            drug1.setVisit(item.getPatientBill().getVisit());

            dispensedDrugRepository.save(drug1);

            stock.setAmount(NumberUtils.toScaledBigDecimal((qty) * (drugs.getPrice())));
            stock.setDeliveryNumber(drug1.getOtherReference());
            stock.setQuantity(qty);
            stock.setItem(drug1.getDrug());
            stock.setMoveType(MovementType.Returns);
            stock.setPrice(NumberUtils.createBigDecimal(String.valueOf(drug1.getAmount())));
            stock.setPurpose(MovementPurpose.Returns);
            stock.setReferenceNumber(drug1.getOtherReference());
            stock.setStore(drug1.getStore());
            stock.setTransactionDate(LocalDate.now());
            stock.setTransactionNumber(trdId);
            stock.setUnit(drug1.getUnits());
            inventoryService.doStockEntry(InventoryEvent.Type.Increase, stock, drug1.getStore(), drug1.getDrug(), qty);

            drugs.setReturnedQuantity((drugs.getReturnedQuantity() + qty));
            drugs.setReturnReason("Bill Adjustments");
            drugs.setReturnDate(item.getBillingDate() != null ? item.getBillingDate() : LocalDate.now());

            dispensedDrugRepository.save(drugs);

        }

    }

    private void doStockEntries(Long drugId) {
        Optional<DispensedDrug> drug = dispensedDrugRepository.findById(drugId);
        if (drug.isPresent()) {
            inventoryService.save(StockEntry.create(drug.get()));
        } else {
            System.err.println("Drug entry is empty");
        }
    }
}
