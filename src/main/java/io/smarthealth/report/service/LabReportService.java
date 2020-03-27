/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.report.service;

import io.smarthealth.clinical.laboratory.data.LabRegisterData;
import io.smarthealth.clinical.laboratory.domain.enumeration.LabTestStatus;
import io.smarthealth.clinical.laboratory.service.LaboratoryService;
import io.smarthealth.clinical.record.data.DoctorRequestData;
import io.smarthealth.clinical.record.data.PatientTestsData;
import io.smarthealth.clinical.record.service.DiagnosisService;
import io.smarthealth.clinical.visit.data.VisitData;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.smarthealth.infrastructure.reports.service.JasperReportsService;
import io.smarthealth.organization.person.domain.enumeration.Gender;
import io.smarthealth.organization.person.patient.data.PatientData;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.report.data.ReportData;
import io.smarthealth.report.data.clinical.PatientVisitData;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
@RequiredArgsConstructor
public class LabReportService {
    private final JasperReportsService reportService;
    private final PatientService patientService;
    private final LaboratoryService labService;
    
    private final VisitService visitService;
    
    
    public void getLabStatement(MultiValueMap<String, String>reportParam, ExportFormat format, HttpServletResponse response) throws SQLException, JRException, IOException {
        ReportData reportData = new ReportData();
        String visitNumber = reportParam.getFirst("visitNumber");
        String labNumber = reportParam.getFirst("labNumber");
        String orderNumber = reportParam.getFirst("orderNumber");
        String patientNumber = reportParam.getFirst("patientNumber");
        String search = reportParam.getFirst("search");
        String dateRange = reportParam.getFirst("dateRange");
        Integer page = Integer.getInteger(reportParam.getFirst("page"));
        Integer size = Integer.getInteger(reportParam.getFirst("size"));
        LabTestStatus status = LabTestStatus.valueOf(reportParam.getFirst("ststus"));
        Pageable pageable = PaginationUtil.createPage(page, size);
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);
        Boolean expand = Boolean.parseBoolean(reportParam.getFirst("summarized"));
        
         List<LabRegisterData> patientData = labService.getLabRegister(labNumber, orderNumber, visitNumber, patientNumber, status,range, search,pageable)
                .getContent()
                .stream()
                .map((register) -> register.toData(expand))
                .collect(Collectors.toList());
        reportData.getFilters().put("SUBREPORT_DIR", "/clinical/");
        reportData.setData(patientData);
        reportData.setFormat(format);
        if(!expand)
            reportData.setTemplate("/clinical/lab/lab_statement");
        else
            reportData.setTemplate("/clinical/lab/lab_statement_summary");
        reportData.setReportName("Lab-Statement");
        reportService.generateReport(reportData, response);
    }    
    
}