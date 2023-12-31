/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.report.service;

import io.smarthealth.accounting.billing.domain.PatientBillItem;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.clinical.laboratory.data.LabRegisterData;
import io.smarthealth.clinical.laboratory.data.LabRegisterTestData;
import io.smarthealth.clinical.laboratory.domain.enumeration.LabTestStatus;
import io.smarthealth.clinical.laboratory.service.LabConfigurationService;
import io.smarthealth.clinical.laboratory.service.LaboratoryService;
import io.smarthealth.clinical.radiology.domain.TotalTest;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.smarthealth.infrastructure.reports.service.JasperReportsService;
import io.smarthealth.organization.person.domain.WalkIn;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.organization.person.service.WalkingService;
import io.smarthealth.report.data.ReportData;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRSortField;
import net.sf.jasperreports.engine.design.JRDesignSortField;
import net.sf.jasperreports.engine.type.SortFieldTypeEnum;
import net.sf.jasperreports.engine.type.SortOrderEnum;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
@RequiredArgsConstructor
public class LabReportService {

    private final JasperReportsService reportService;
    private final BillingService billingService;
    private final LaboratoryService labService;

    private final VisitService visitService;
    private final LabConfigurationService labSetUpService;
    private final WalkingService walkinService;

    public void getLabStatement(MultiValueMap<String, String> reportParam, ExportFormat format, HttpServletResponse response) throws SQLException, JRException, IOException {
        ReportData reportData = new ReportData();
        String visitNumber = reportParam.getFirst("visitNumber");
        String labNumber = reportParam.getFirst("labNumber");
        String orderNumber = reportParam.getFirst("orderNumber");
        String patientNumber = reportParam.getFirst("patientNumber");
        String search = reportParam.getFirst("search");
        String dateRange = reportParam.getFirst("dateRange");
        Boolean isWalkin = reportParam.getFirst("iswalkin") != null ? Boolean.parseBoolean(reportParam.getFirst("iswalkin")) : null;
        List<LabTestStatus> status = Arrays.asList(statusToEnum(reportParam.getFirst("status")));
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);
        Boolean expand = Boolean.parseBoolean(reportParam.getFirst("summarized"));

        List<LabRegisterData> patientData = labService.getLabRegister(labNumber, orderNumber, visitNumber, patientNumber, status, range, search, Pageable.unpaged())
                .getContent()
                .stream()
                .map((register) -> {
                    LabRegisterData data = register.toData(expand);
                    if (data.getIsWalkin() != null) {
                        if (data.getIsWalkin()) {
                            Optional<WalkIn> walkin = walkinService.fetchWalkingByWalkingNo(register.getPatientNo());
                            if (walkin.isPresent()) {
                                data.setPatientName(walkin.get().getFullName());
                            }
                        }
                    }
                    return data;
                })
                .collect(Collectors.toList());
        reportData.setData(patientData);
        reportData.setPatientNumber(patientNumber);
        reportData.setFormat(format);
        if (!expand) {
            reportData.setTemplate("/clinical/laboratory/LabStatement");
        } else {
            reportData.setTemplate("/clinical/laboratory/LabStatement_summary");
        }

        Instant fromDate = range.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toDate = range.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TotalTest> tests = labService.getPatientTestTotals(fromDate, toDate);

        reportData.setReportName("Lab-Statement");
        reportData.getFilters().put("tests", tests);
        reportData.getFilters().put("range", DateRange.getReportPeriod(range));
        reportService.generateReport(reportData, response);
    }

    public void getLabTestStatement(MultiValueMap<String, String> reportParam, ExportFormat format, HttpServletResponse response) throws SQLException, JRException, IOException {
        ReportData reportData = new ReportData();
        String visitNumber = reportParam.getFirst("visitNumber");
        String labNumber = reportParam.getFirst("labNumber");
        String orderNumber = reportParam.getFirst("orderNumber");
        String patientNumber = reportParam.getFirst("patientNumber");
        String search = reportParam.getFirst("search");
        String dateRange = reportParam.getFirst("dateRange");
        LabTestStatus status = labService.LabTestStatusToEnum(reportParam.getFirst("status"));
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);
        Boolean expand = Boolean.parseBoolean(reportParam.getFirst("summarized"));
        Boolean isWalkin = reportParam.getFirst("isWalkin") != null ? BooleanUtils.toBoolean(reportParam.getFirst("isWalkin")) : null;

        List<LabRegisterTestData> patientData = labService.getLabRegisterTest(labNumber, orderNumber, visitNumber,
                patientNumber, status, range, isWalkin, search, null,Pageable.unpaged())
                .getContent()
                .stream()
                .map((register) -> {
                    LabRegisterTestData data = register.toData(expand);
                    if (data.getIsWalkin() != null) {
                        if (data.getIsWalkin()) {
                            Optional<WalkIn> walkin = walkinService.fetchWalkingByWalkingNo(register.getLabRegister().getPatientNo());
                            if (walkin.isPresent()) {
                                data.setPatientName(walkin.get().getFullName());
                            }
                        }
                    }

                    List<PatientBillItem> billItem = billingService.getPatientBillItem(data.getReferenceNo());
                    if (!billItem.isEmpty()) {
                        data.setReferenceNo(billItem.get(0).getPaymentReference());
                        billItem.stream().map((item) -> {
                            if (billItem.get(0).getPatientBill().getPaymentMode().equals("Cash") || billItem.get(0).getPatientBill().getPaymentMode() == null) {
                                data.setTotalCash(data.getTotalCash() + item.getAmount());
                            } else {
                                data.setTotalInsurance(data.getTotalInsurance() + item.getAmount());
                            }
                            return item;
                        });
                    }
                    return data;
                })
                .collect(Collectors.toList());

        Instant fromDate = range.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toDate = range.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TotalTest> tests = labService.getPatientTestTotals(fromDate, toDate);
        List<TotalTest> requests = labService.getTotalRequests(fromDate, toDate);

        reportData.setData(patientData);

        reportData.setFormat(format);
        if (!expand) {
            reportData.setTemplate("/clinical/laboratory/LabStatement");
        } else {
            reportData.setTemplate("/clinical/laboratory/LabStatement_summary");
        }
        List<JRSortField> sortList = new ArrayList<>();
        JRDesignSortField sortField = new JRDesignSortField();
        sortField.setName("status");
        sortField.setOrder(SortOrderEnum.ASCENDING);
        sortField.setType(SortFieldTypeEnum.FIELD);
        reportData.setPatientNumber(patientNumber);
        sortList.add(sortField);
        reportData.getFilters().put(JRParameter.SORT_FIELDS, sortList);

        reportData.getFilters().put("tests", tests);
        reportData.getFilters().put("requests", requests);
        reportData.getFilters().put("range", DateRange.getReportPeriod(range));
        reportData.setReportName("Lab-Statement");
        reportService.generateReport(reportData, response);
    }

    public void getPatientLabReport(MultiValueMap<String, String> reportParam, ExportFormat format, HttpServletResponse response) throws SQLException, JRException, IOException {
        String labNumber = reportParam.getFirst("labNumber");
//        Visit visit = visitService.findVisitEntityOrThrow("O000005");
        LabRegisterData labTests = labService.getLabRegisterByNumber(labNumber).toData(Boolean.TRUE);//tLabResultDataByVisit(visit);
        System.out.println("Report Data "+labTests.toString());
        ReportData reportData = new ReportData();
        List<JRSortField> sortList = new ArrayList();
        JRDesignSortField sortField = new JRDesignSortField();
        sortField.setName("visitNumber");
        sortField.setOrder(SortOrderEnum.ASCENDING);
        sortField.setType(SortFieldTypeEnum.FIELD);
        sortList.add(sortField);
        reportData.getFilters().put(JRParameter.SORT_FIELDS, sortList);
        reportData.setPatientNumber(labTests.getPatientNo());
        reportData.setData(Arrays.asList(labTests));
        reportData.setEmployeeId(labTests.getRequestedByStaffNumber());
        reportData.setFormat(format);
        reportData.setTemplate("/clinical/laboratory/patientLab_report");
        reportData.setReportName("Lab-report");
        reportService.generateReport(reportData, response);
    }

    public void genSpecimenLabel(MultiValueMap<String, String> reportParam, ExportFormat format, HttpServletResponse response) throws SQLException, JRException, IOException {
        ReportData reportData = new ReportData();
        Long testId = NumberUtils.createLong(reportParam.getFirst("labRegisterTestId"));
        LabRegisterTestData testData = labService.getLabRegisterTest(testId).toData(false);

        reportData.setData(Arrays.asList(testData));
        reportData.setFormat(format);
        reportData.setTemplate("/clinical/laboratory/specimen_label");
        reportData.setReportName("specimen-label");
        reportService.generateReport(reportData, response);
    }

    private LabTestStatus statusToEnum(String status) {
        if (status == null || status.equals("null") || status.equals("")) {
            return null;
        }
        if (EnumUtils.isValidEnum(LabTestStatus.class, status)) {
            return LabTestStatus.valueOf(status);
        }
        throw APIException.internalError("Provide a Valid Bill Status");
    }
}
