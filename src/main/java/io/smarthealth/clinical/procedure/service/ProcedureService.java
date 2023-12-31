/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.procedure.service;

import io.smarthealth.accounting.billing.domain.PatientBill;
import io.smarthealth.accounting.billing.domain.PatientBillItem;
import io.smarthealth.accounting.billing.domain.enumeration.BillEntryType;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.clinical.procedure.data.PatientProcedureRegisterData;
import io.smarthealth.clinical.procedure.data.ProcedureItemData;
import io.smarthealth.clinical.procedure.data.ProcedureData;
import io.smarthealth.clinical.procedure.domain.PatientProcedureRegister;
import io.smarthealth.clinical.procedure.domain.PatientProcedureTest;
import io.smarthealth.clinical.procedure.domain.ProcedureRepository;
import io.smarthealth.clinical.procedure.domain.Procedure;
import io.smarthealth.clinical.procedure.domain.ProcedureTestRepository;
import io.smarthealth.clinical.procedure.domain.enumeration.ProcedureTestState;
import io.smarthealth.clinical.procedure.domain.specification.ProcedureSpecification;
import io.smarthealth.clinical.radiology.domain.TotalTest;
import io.smarthealth.clinical.record.data.enums.FullFillerStatusType;
import io.smarthealth.clinical.record.domain.DoctorRequest;
import io.smarthealth.clinical.record.domain.DoctorsRequestRepository;
import io.smarthealth.clinical.visit.data.enums.VisitEnum;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.facility.service.EmployeeService;
import io.smarthealth.sequence.SequenceNumberService;
import io.smarthealth.sequence.Sequences;
import io.smarthealth.stock.item.domain.Item;
import io.smarthealth.stock.item.service.ItemService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.smarthealth.clinical.procedure.domain.ProcedureRegisterRepository;
import io.smarthealth.clinical.procedure.domain.RegisterTestRepository;
import io.smarthealth.clinical.procedure.domain.specification.RegisterTestSpecification;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
@RequiredArgsConstructor
public class ProcedureService {

    private final ProcedureRepository procedureRepository;

    private final ProcedureRegisterRepository patientprocedureRepository;
    private final RegisterTestRepository registerTestRepository;

    private final DoctorsRequestRepository doctorRequestRepository;

    private final ProcedureTestRepository procTestRepository;

    private final EmployeeService employeeService;
    private final ItemService itemService;
    private final VisitService visitService;
    private final SequenceNumberService sequenceNumberService;
    private final BillingService billingService;
    private final ServicePointService servicePointService;

    @Transactional
    public List<Procedure> createProcedureTest(List<ProcedureData> procedureTestData) {
        try {
            List<Procedure> procedureTests = procedureTestData
                    .stream()
                    .map((procedureTest) -> {
                        Procedure test = ProcedureData.map(procedureTest);
                        Optional<Item> item = itemService.findByItemCode(procedureTest.getItemCode());
                        if (item.isPresent()) {
                            test.setItem(item.get());
                        }
                        return test;
                    })
                    .collect(Collectors.toList());
            return procedureRepository.saveAll(procedureTests);
        } catch (Exception e) {
            e.printStackTrace();
            throw APIException.internalError("Error occured while creating ProcedureItem ", e.getMessage());
        }
    }

    @Transactional
    public Procedure UpdateProcedureTest(ProcedureData procedureTestData) {
        Procedure procedureTest = this.getById(procedureTestData.getId());
        Procedure test = ProcedureData.map(procedureTestData);
        test.setId(procedureTest.getId());
        Optional<Item> item = itemService.findByItemCode(procedureTestData.getItemCode());
        if (item.isPresent()) {
            test.setItem(item.get());
        }

        return procedureRepository.save(test);

    }

    public Procedure findProcedureByItem(final Item item) {
        return procedureRepository.findByItem(item).orElseThrow(() -> {
            return APIException.notFound("Procedure Test not Registered in Procedure Department");
        });
    }

    @Transactional
    public Procedure getById(Long id) {
        return procedureRepository.findById(id).orElseThrow(() -> APIException.notFound("Procedure Test identified by {0} not found", id));
    }

    @Transactional
    public Page<Procedure> findAll(Pageable pgbl) {
        return procedureRepository.findAll(pgbl);
    }

    @Transactional
    public PatientProcedureRegister savePatientProcedure(PatientProcedureRegisterData patientProcRegData, final String visitNo) {
        PatientProcedureRegister patientProcReg = PatientProcedureRegisterData.map(patientProcRegData);
        String transactionId = sequenceNumberService.next(1L, Sequences.Transactions.name());
        patientProcReg.setTransactionId(transactionId);
        Visit visit = null;
        if (patientProcRegData.getIsWalkin()) {
            patientProcReg.setPatientName(patientProcRegData.getPatientName());
            patientProcReg.setPatientNo(patientProcRegData.getPatientNumber());
            patientProcReg.setIsWalkin(Boolean.TRUE);
            patientProcReg.setRequestedBy(patientProcRegData.getRequestedBy());
        } else {
             visit = visitService.findVisitEntityOrThrow(visitNo);
            patientProcReg.setVisit(visit);            
            patientProcReg.setPatientName(visit.getPatient().getFullName());
            patientProcReg.setPatientNo(visit.getPatient().getPatientNumber());
            patientProcReg.setIsWalkin(Boolean.FALSE);
            if(visit.getHealthProvider()!=null)
                patientProcReg.setRequestedBy(visit.getHealthProvider().getFullName());
        }

//        Optional<Employee> emp = employeeService.findEmployeeByStaffNumber(patientProcRegData.getRequestedBy());
//        if (emp.isPresent()) {
//            patientProcReg.setRequestedBy(emp.get());
//        }

        if (patientProcRegData.getAccessionNo() == null || patientProcRegData.getAccessionNo().equals("")) {
            String accessionNo = sequenceNumberService.next(1L, Sequences.Procedure.name());
            patientProcReg.setAccessNo(accessionNo);
        }

        //PatientTestRegister savedPatientTestRegister = patientRegRepository.save(patientTestReg);
        double amount = 0;
        if (!patientProcRegData.getItemData().isEmpty()) {
            List<PatientProcedureTest> patientProcTest = new ArrayList<>();
            for (ProcedureItemData id : patientProcRegData.getItemData()) {
                Item item = itemService.findItemWithNoFoundDetection(id.getItemCode());
                PatientProcedureTest pte = new PatientProcedureTest();
                amount = amount + id.getItemPrice();
                pte.setStatus(ProcedureTestState.Scheduled);
                pte.setTestPrice(id.getItemPrice());
                pte.setQuantity(id.getQuantity());

                if(id.isPaid() || patientProcRegData.getPaymentMode().equals("Insurance")){
                    pte.setPaid(true);
                }else {
                    if (patientProcRegData.getPaymentMode().equals("Cash") && visit != null && visit.getVisitType() == VisitEnum.VisitType.Outpatient) {
                        pte.setPaid(Boolean.FALSE);
                    } else {
                        pte.setPaid(Boolean.TRUE);
                    }
                }
                pte.setProcedureTest(item);
                pte.setPaymentMethod(id.getPaymentMethod());
                pte.setMedic(employeeService.findEmployeeById(id.getMedicId()));
//                Optional<Employee> employee = employeeService.findEmployeeById(id.getMedicId());
//                if (employee.isPresent()) {
//                    pte.setMedic(employee.get());
//                }
                if (id.getRequestItemId() != null) {
                    Optional<DoctorRequest> request = doctorRequestRepository.findById(id.getRequestItemId());
                    if (request.isPresent()) {
                        pte.setRequest(request.get());
                        request.get().setFulfillerStatus(FullFillerStatusType.Fulfilled);
                    }
                }
                pte.setBilled(id.isRequestBilled());

                patientProcTest.add(pte);

            }

            patientProcReg.addPatientProcedures(patientProcTest);

        }
        patientProcReg.setAmount(amount);
        patientProcReg.setBalance(amount);
        PatientProcedureRegister savedProcedure = patientprocedureRepository.save(patientProcReg);

        PatientBill bill = toBill(savedProcedure,patientProcRegData);
        //save the bill
        billingService.save(bill);
        return savedProcedure;
    }

    private PatientBill toBill(PatientProcedureRegister data, PatientProcedureRegisterData patientProcRegData) {
        ServicePoint servicePoint = servicePointService.getServicePointByType(ServicePointType.Procedure);
        PatientBill patientbill = new PatientBill();

        if (!data.getIsWalkin()) {
            patientbill.setVisit(data.getVisit());
            patientbill.setPatient(data.getVisit().getPatient());
            patientbill.setWalkinFlag(Boolean.FALSE);
            patientbill.setPaymentMode(data.getVisit().getPaymentMethod().name());
        } else {
            patientbill.setReference(data.getPatientNo());
            patientbill.setOtherDetails(data.getPatientName());
            patientbill.setWalkinFlag(Boolean.TRUE);
            patientbill.setPaymentMode(PaymentMethod.Cash.name());
        }
        patientbill.setAmount(data.getAmount());
        patientbill.setBillingDate(data.getBillingDate());
        patientbill.setDiscount(data.getDiscount());
        patientbill.setBalance(data.getAmount());
        patientbill.setTransactionId(data.getTransactionId());
        patientbill.setStatus(BillStatus.Draft);

        patientbill.setSurpassedAmountPaymentMethod(patientProcRegData.getSurpassedAmountPaymentMethod());
        patientbill.setAllowedToExceedLimit(patientProcRegData.getAllowedToExceedLimit());

        String bill_no = sequenceNumberService.next(1L, Sequences.BillNumber.name());

        patientbill.setBillNumber(bill_no);
        List<PatientBillItem> lineItems = data.getPatientProcedureTest()
                .stream()
                .filter(x -> !x.isBilled())
                .map(lineData -> {
                    PatientBillItem billItem = new PatientBillItem();
                    Item item = itemService.findByItemCodeOrThrow(lineData.getProcedureTest().getItemCode());

                    billItem.setTransactionId(data.getTransactionId());
                    billItem.setServicePoint(ServicePointType.Procedure.name());
                    if (lineData.getMedic() != null) {
                        billItem.setMedicId(lineData.getMedic().getId());
                    }
                    billItem.setRequestReference(lineData.getId());
                    billItem.setItem(item);
                    billItem.setPaid(Boolean.FALSE);
                    billItem.setPrice(lineData.getTestPrice());
                    billItem.setQuantity(lineData.getQuantity());
                    billItem.setAmount(lineData.getTestPrice() * lineData.getQuantity());
                    billItem.setBalance(lineData.getTestPrice() * lineData.getQuantity());
                    billItem.setServicePoint(servicePoint.getName());
                    billItem.setServicePointId(servicePoint.getId());
                    billItem.setStatus(BillStatus.Draft);
                    billItem.setBillingDate(data.getBillingDate());
                    billItem.setEntryType(BillEntryType.Debit);
                    billItem.setBillPayMode(lineData.getPaymentMethod());

                    return billItem;
                })
                .collect(Collectors.toList());
        patientbill.addBillItems(lineItems);
        return patientbill;
    }

    @Transactional
    public PatientProcedureTest updateProcedureResult(PatientProcedureTest r) {
        return procTestRepository.save(r);
    }

    public PatientProcedureTest findResultsByIdWithNotFoundDetection(Long id) {
        return registerTestRepository.findById(id).orElseThrow(() -> APIException.notFound("Procedure identified by id {0} not found ", id));
    }

    public PatientProcedureRegister findProceduresByIdWithNotFoundDetection(String accessNo) {
        return patientprocedureRepository.findByAccessNo(accessNo).orElseThrow(() -> APIException.notFound("Patient Procedure identified by procedure Number {0} not found ", accessNo));
    }

    public List<PatientProcedureRegister> findPatientProcedureRegisterByVisit(String VisitNumber) {
        Visit visit = visitService.findVisitEntityOrThrow(VisitNumber);
        return patientprocedureRepository.findByVisit(visit);
    }

    public Page<PatientProcedureTest> findPatientProcedureTests(String PatientNumber, String scanNo, String visitId, ProcedureTestState status, Boolean isWalkin, DateRange range, Pageable pgbl) {
        Specification spec = RegisterTestSpecification.createSpecification(PatientNumber, scanNo, visitId, status, isWalkin, range);
        return registerTestRepository.findAll(spec, pgbl);
    }

    @Transactional
    public Page<PatientProcedureRegister> findAll(String PatientNumber, String scanNo, String visitId, DateRange range, Pageable pgbl) {
        Specification spec = ProcedureSpecification.createSpecification(PatientNumber, scanNo, visitId, range);
        return patientprocedureRepository.findAll(spec, pgbl);
    }

    public List<PatientProcedureTest> findProcedureResultsByVisit(Visit visit) {
        return registerTestRepository.findByVisit(visit);
    }

    public List<TotalTest> getPatientTestTotals(Instant fromDate, Instant toDate) {
        return procTestRepository.findTotalTests(fromDate, toDate);
    }

    public List<TotalTest> getTotalRequests(Instant fromDate, Instant toDate) {
        return procTestRepository.findTotalTestsByPractitioner(fromDate, toDate);
    }
}
