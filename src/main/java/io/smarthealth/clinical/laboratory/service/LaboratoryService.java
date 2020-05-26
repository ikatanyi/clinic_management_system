package io.smarthealth.clinical.laboratory.service;

import io.smarthealth.accounting.billing.domain.PatientBill;
import io.smarthealth.accounting.billing.domain.PatientBillItem;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.clinical.laboratory.data.LabRegisterData;
import io.smarthealth.clinical.laboratory.data.LabRegisterTestData;
import io.smarthealth.clinical.laboratory.data.LabResultData;
import io.smarthealth.clinical.laboratory.data.PatientResults;
import io.smarthealth.clinical.laboratory.data.StatusRequest;
import io.smarthealth.clinical.laboratory.domain.LabRegister;
import io.smarthealth.clinical.laboratory.domain.LabRegisterTest;
import io.smarthealth.clinical.laboratory.domain.LabResult;
import io.smarthealth.clinical.laboratory.domain.LabResultRepository;
import io.smarthealth.clinical.laboratory.domain.LabTest;
import io.smarthealth.clinical.laboratory.domain.LabTestRepository;
import io.smarthealth.clinical.laboratory.domain.enumeration.LabTestStatus;
import io.smarthealth.clinical.laboratory.domain.specification.LabRegisterSpecification;
import io.smarthealth.clinical.laboratory.domain.specification.LabResultSpecification;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.VisitRepository;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.sequence.SequenceNumberService;
import io.smarthealth.sequence.Sequences;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.smarthealth.clinical.laboratory.domain.LabRegisterRepository;
import io.smarthealth.clinical.laboratory.domain.LabRegisterTestRepository;
import io.smarthealth.clinical.laboratory.domain.specification.LabRegisterTestSpecification;
import io.smarthealth.clinical.record.data.DoctorRequestData;
import io.smarthealth.clinical.record.data.enums.FullFillerStatusType;
import io.smarthealth.clinical.record.domain.DoctorRequest;
import io.smarthealth.clinical.record.domain.DoctorsRequestRepository;
import io.smarthealth.notifications.service.RequestEventPublisher;
import io.smarthealth.organization.person.domain.WalkIn;
import io.smarthealth.organization.person.service.WalkingService;
import io.smarthealth.security.util.SecurityUtils;
import io.smarthealth.stock.item.domain.Item;
import java.time.LocalDate;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.transaction.annotation.Propagation;

/**
 *
 * @author Kelsas
 */
@Service
@RequiredArgsConstructor
public class LaboratoryService {

    private final LabRegisterRepository repository;
    private final LabTestRepository labTestRepository;
    private final VisitRepository visitRepository;
    private final LabRegisterTestRepository testRepository;
    private final SequenceNumberService sequenceNumberService;
    private final LabResultRepository labResultRepository;
    private final WalkingService walkingService;
    private final ServicePointService servicePointService;
    private final BillingService billingService;
    private final DoctorsRequestRepository doctorRequestRepository;
    private final RequestEventPublisher requestEventPublisher;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public LabRegister createLabRegister(LabRegisterData data) {

        LabRegister request = toLabRegister(data);

        String trnId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        String labNo = sequenceNumberService.next(1L, Sequences.LabNumber.name());
        request.setLabNumber(labNo);
        request.setTransactionId(trnId);
        data.setTransactionId(trnId);

        LabRegister savedRegister = repository.save(request);

//        billingService.save(toBill(data));
        billingService.save(toBill(savedRegister));
        //save
        data.getTests()
                .stream()
                .forEach(x -> {
                    fulfillDocRequest(x.getRequestId());
                });
        requestEventPublisher.publishUpdateEvent(DoctorRequestData.RequestType.Laboratory);
        return savedRegister;
    }

    public LabRegister getLabRegisterByNumber(String labNo) {
        return repository.findByLabNumber(labNo)
                .orElseThrow(() -> APIException.notFound("Lab Test with LabNumber {0} Not Found", labNo));
    }

    public LabRegister getLabRegisterById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> APIException.notFound("Lab Request with id {0} Not Found", id));
    }

    public LabRegister updateLabRegister(Long id, LabRegisterData data) {
        LabRegister requests = getLabRegisterById(id);

        return requests;//repository.save(requests);
    }

    @Transactional
    public int updateLabRegisteredTest(String labNo, Long testId, StatusRequest status) {
        LabRegister requests = getLabRegisterByNumber(labNo);

        LabRegisterTest test = requests.getTests()
                .stream()
                .filter(x -> Objects.equals(x.getId(), testId))
                .findAny()
                .orElseThrow(() -> APIException.notFound("Lab Test with Id {0} Not Found", testId));

        switch (status.getStatus()) {
            case Collected:
                repository.updateLabRegisterStatus(LabTestStatus.PendingResult, requests.getId());
                return testRepository.updateTestCollected(status.getDoneBy(), status.getSpecimen(), testId, LabTestStatus.PendingResult);
            case Entered:
                repository.updateLabRegisterStatus(LabTestStatus.ResultsEntered, requests.getId());
                updateRegisterStatus(requests);
                return testRepository.updateTestEntry(status.getDoneBy(), testId, LabTestStatus.ResultsEntered);
            case Validated:
                repository.updateLabRegisterStatus(LabTestStatus.Complete, requests.getId());
                return testRepository.updateTestValidation(status.getDoneBy(), testId, LabTestStatus.Complete);
            case Paid:
//                test.setPaid(Boolean.TRUE);
                return testRepository.updateTestPaid(testId);
            default:
                return 0;
        }
    }

    public void voidLabRegister(Long id) {
        LabRegister requests = getLabRegisterById(id);
        requests.setVoided(Boolean.TRUE);
        repository.save(requests);
    }

    @Transactional
    private void updateRegisterStatus(LabRegister requests) {

        if (requests.isCompleted()) {
            repository.updateLabRegisterStatus(LabTestStatus.Complete, requests.getId());
        } else {
            repository.updateLabRegisterStatus(LabTestStatus.PartialResult, requests.getId());
        }
    }

    public Page<LabRegister> getLabRegister(String labNumber, String orderNumber, String visitNumber, String patientNumber, List<LabTestStatus> status, DateRange range, String search, Pageable page) {
        Specification<LabRegister> spec = LabRegisterSpecification.createSpecification(labNumber, orderNumber, visitNumber, patientNumber, status, range, search);
        return repository.findAll(spec, page);
    }

    public Page<LabRegisterTest> getLabRegisterTest(String labNumber, String orderNumber, String visitNumber, String patientNumber, LabTestStatus status, DateRange range, String search, Pageable page) {
        Specification<LabRegisterTest> spec = LabRegisterTestSpecification.createSpecification(labNumber, orderNumber, visitNumber, patientNumber, status, range, search);
        return testRepository.findAll(spec, page);
    }

//RESULTS
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public LabResult createLabResult(LabResultData data) {
        LabResult result = toLabResult(data);
        LabResult savedResult = labResultRepository.save(result);
        updateResultsEntry(savedResult);
        return savedResult;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<LabResult> createLabResult(List<LabResultData> data) {

        List<LabResult> toSave = data
                .stream()
                .map(x -> toLabResult(x))
                .collect(Collectors.toList());

        List<LabResult> savedResults = labResultRepository.saveAll(toSave);

        savedResults.forEach(x -> updateResultsEntry(x));

        return savedResults;
    }

    public LabResult getResult(Long id) {
        return labResultRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("Result with Id  {0} Not Found", id));
    }

    public LabResult updateLabResult(Long id, LabResultData data) {
        LabResult result = getResult(id);
        return result;
    }

    public void voidLabResult(Long id) {
        LabResult result = getResult(id);
        result.setVoided(Boolean.TRUE);
        labResultRepository.save(result);
    }

    public List<LabResultData> getLabResultDataByVisit(Visit visit) {
        return getResultByVisit(visit)
                .stream()
                .map(x -> x.toData())
                .collect(Collectors.toList());
    }

    public List<LabResult> getResultByVisit(Visit visit) {
        return labResultRepository.findByVisit(visit);
    }

    public List<LabResult> getResultByRegister(LabRegister register) {
        return labResultRepository.findByLabRegisterNumber(register);
    }

    public Page<LabResult> getLabResults(String visitNumber, String patientNumber, String labNumber, Boolean walkin, String testName, String orderNumber, DateRange range, Pageable page) {
        Specification<LabResult> spec = LabResultSpecification.createSpecification(visitNumber, patientNumber, labNumber, walkin, testName, orderNumber, range);
        return labResultRepository.findAll(spec, page);
    }

    @Transactional
    private void updateResultsEntry(LabResult savedResult) {

        LabRegisterTest test = savedResult.getLabRegisterTest();
        testRepository.updateTestEntry(SecurityUtils.getCurrentUserLogin().orElse("system"), test.getId(), LabTestStatus.ResultsEntered);

        updateRegisterStatus(test.getLabRegister());
    }

    private LabResult toLabResult(LabResultData data) {
        LabRegisterTest labRequestTest = getLabRegisterTest(data.getLabRegisterTestId());

        LabResult results = new LabResult();
        results.setAnalyte(data.getAnalyte());
        results.setLabNumber(data.getLabNumber());
        results.setLabRegisterTest(labRequestTest);
        results.setLowerLimit(data.getLowerLimit());
        results.setPatientNo(data.getPatientNo());
        results.setReferenceValue(data.getReferenceValue());
        results.setResultValue(data.getResultValue());
        results.setResultsDate(LocalDateTime.now());
        results.setUnits(data.getUnits());
        results.setUpperLimit(data.getUpperLimit());
        return results;
    }

    private LabRegister toLabRegister(LabRegisterData data) {
        LabRegister request = new LabRegister();
        request.setOrderNumber(data.getOrderNumber());
        request.setIsWalkin(data.getIsWalkin());
        request.setPaymentMode(data.getPaymentMode());
        if (!data.getIsWalkin()) {
            Visit visit = getPatientVisit(data.getVisitNumber());
            request.setVisit(visit);
            request.setRequestedBy(data.getRequestedBy());
            request.setPatientNo(visit.getPatient().getPatientNumber());
        } else {
            WalkIn w = createWalking(data.getPatientName());
            request.setRequestedBy(data.getPatientName());
            request.setPatientNo(w.getWalkingIdentitificationNo());
            request.setPaymentMode("Cash");
        }
        request.setRequestDatetime(data.getRequestDatetime());
        request.setStatus(LabTestStatus.AwaitingSpecimen);

        request.addPatientTest(data.getTests()
                .stream()
                .map(x -> toLabRegisterTest(x, data.getPaymentMode()))
                .collect(Collectors.toList())
        );

        return request;
    }

    private LabRegisterTest toLabRegisterTest(LabRegisterTestData data, String paymentMode) {
        LabTest labTest = getLabTest(data.getTestId());
        LabRegisterTest test = new LabRegisterTest();
        test.setCollected(Boolean.FALSE);
        test.setEntered(Boolean.FALSE);
        test.setLabTest(labTest);

        test.setPaid(paymentMode.equals("Cash") ? Boolean.FALSE : Boolean.TRUE);

        test.setVoided(Boolean.FALSE);
        test.setValidated(Boolean.FALSE);
        test.setRequestId(data.getRequestId());
        test.setPrice(data.getTestPrice());
        test.setStatus(data.getStatus());

        test.setReferenceNo(data.getReferenceNo());

        test.setStatus(LabTestStatus.AwaitingSpecimen);
        return test;
    }

    private LabTest getLabTest(Long id) {
//        return labTestRepository.findById(id)
        return labTestRepository.findByItemId(id)
                .orElseThrow(() -> APIException.notFound("Lab Test with Id {0} Not Found", id));
    }

    private Visit getPatientVisit(String visitNo) {
        return visitRepository.findByVisitNumber(visitNo)
                .orElseThrow(() -> APIException.notFound("Visit with Id {0} Not Found", visitNo));
    }

    private LabRegisterTest getLabRegisterTest(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("Lab Request Test with Id {0} Not Found", id));
    }

    public PatientResults getPatientResults(String patientNo, String visitNumber) {
        Specification<LabRegister> spec = LabRegisterSpecification.createSpecification(null, null, visitNumber, null, null, null, null);
        List<LabRegister> lists = repository.findAll(spec);
        PatientResults results = new PatientResults();

        return results;
    }

    public List<LabRegisterTest> getTestsResultsByVisit(String visitNo, String labNumber) {
        if (labNumber != null) {
            return testRepository.findTestsByVisitAndLabNo(visitNo, labNumber);
        }
        return testRepository.findTestsByVisitNumber(visitNo);
    }

    private WalkIn createWalking(String patientName) {
        WalkIn w = new WalkIn();
        w.setFirstName(patientName);
        w.setSurname("WI");
        return walkingService.createWalking(w);
    }

    private PatientBill toBill(/*LabRegisterData*/LabRegister data) {
        //get the service point from store
        Visit visit = data.getVisit();// visitRepository.findByVisitNumber(data.getVisitNumber()).orElse(null);
        //find the service point for lab
        ServicePoint srvpoint = servicePointService.getServicePointByType(ServicePointType.Laboratory);

        PatientBill patientbill = new PatientBill();
        patientbill.setVisit(visit);
        if (visit != null) {
            patientbill.setPatient(visit.getPatient());
            patientbill.setWalkinFlag(Boolean.FALSE);
        }
        // how do I deal with bills for Walkin
        if (data.getIsWalkin()) {
            patientbill.setReference(data.getPatientNo());
            patientbill.setOtherDetails(data.getRequestedBy());
            patientbill.setWalkinFlag(Boolean.TRUE);
        }

        patientbill.setBillingDate(LocalDate.now());
//        patientbill.setReferenceNo(data.getReferenceNo());
        patientbill.setPaymentMode(data.getPaymentMode() != null ? data.getPaymentMode() : "Cash");
        patientbill.setTransactionId(data.getTransactionId());
        patientbill.setStatus(BillStatus.Draft);
        List<PatientBillItem> lineItems = data.getTests()
                .stream()
                .map(lineData -> {
                    PatientBillItem billItem = new PatientBillItem();
//                    Item item = billingService.getItemByBy(lineData.getTestId());
                    Item item = lineData.getLabTest().getService();

                    billItem.setBillingDate(LocalDate.now());
                    billItem.setTransactionId(data.getTransactionId());
                    billItem.setServicePointId(srvpoint.getId());
                    billItem.setServicePoint(srvpoint.getName());
                    billItem.setItem(item);
//                    billItem.setPrice(lineData.getTestPrice().doubleValue());
                    billItem.setPrice(lineData.getPrice().doubleValue());
                    billItem.setQuantity(1d);
//                    billItem.setAmount(lineData.getTestPrice().doubleValue());
                    billItem.setAmount(lineData.getPrice().doubleValue());
                    billItem.setDiscount(0.00);
                    billItem.setPaid(data.getPaymentMode() != null ? data.getPaymentMode().equals("Insurance") : false);
//                    billItem.setBalance(lineData.getTestPrice().doubleValue());
                    billItem.setBalance(lineData.getPrice().doubleValue());
                    billItem.setServicePoint(srvpoint.getName());
                    billItem.setServicePointId(srvpoint.getId());
                    billItem.setStatus(BillStatus.Draft);
                    billItem.setRequestReference(lineData.getId());
                    return billItem;
                })
                .collect(Collectors.toList());
        Double amount = patientbill.getBillTotals();
        patientbill.setAmount(amount);
        patientbill.setDiscount(0D);
        patientbill.setBalance(amount);
        patientbill.addBillItems(lineItems);
        return patientbill;
    }

    private void fulfillDocRequest(Long id) {
        if (id == null) {
            return;
        }
        DoctorRequest req = doctorRequestRepository.findById(id).orElse(null);
        if (req != null) {
            req.setFulfillerStatus(FullFillerStatusType.Fulfilled);
            doctorRequestRepository.save(req);
        }
    }

    public LabTestStatus LabTestStatusToEnum(String status) {
        if (status == null || status.equals("null") || status.equals("")) {
            return null;
        }
        if (EnumUtils.isValidEnum(LabTestStatus.class, status)) {
            return LabTestStatus.valueOf(status);
        }
        throw APIException.internalError("Provide a Valid Invoice Status");
    }
}
