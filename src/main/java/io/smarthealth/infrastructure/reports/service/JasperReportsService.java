package io.smarthealth.infrastructure.reports.service;

import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.smarthealth.ApplicationProperties;
import io.smarthealth.organization.facility.domain.Facility;
import io.smarthealth.organization.facility.service.FacilityService;
import io.smarthealth.report.data.ReportData;
import io.smarthealth.report.storage.StorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.export.AbstractXlsReportConfiguration;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Kelsas
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JasperReportsService  {

    @Autowired
    private final StorageService storageService;

    @Autowired
    private final FacilityService facilityService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    
    public byte[] generatePDFReport(String inputFileName, Map<String, Object> params) {
        return generatePDFReport(ExportFormat.PDF, inputFileName, params, new JREmptyDataSource());
    }

    
    public byte[] generatePDFReport(ExportFormat format, String inputFileName, Map<String, Object> params) {
        return generatePDFReport(format, inputFileName, params, new JREmptyDataSource());
    }

    
    public byte[] generatePDFReport(ExportFormat format, String inputFileName, Map<String, Object> params,
            JRDataSource dataSource) {
        byte[] bytes = null;
        JasperReport jasperReport = null;
        try {
            // Check if a compiled report exists
            if (storageService.jasperFileExists(inputFileName)) {
                jasperReport = (JasperReport) JRLoader.loadObject(storageService.loadJasperFile(inputFileName));
            } // Compile report from source and save
            else {
                String jrxml = storageService.loadJrxmlFile(inputFileName);
                log.info("{} loaded. Compiling report", jrxml);
                jasperReport = JasperCompileManager.compileReport(jrxml);
                // Save compiled report. Compiled report is loaded next time
                JRSaver.saveObject(jasperReport, storageService.loadJasperFile(inputFileName));
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            if (format == ExportFormat.PDF) {
                bytes = generatePDF(jasperPrint);
            } else if (format == ExportFormat.XLSX) {
                bytes = generateExcel(jasperPrint);
            }
        } catch (JRException e) {
            log.error("Encountered error when loading jasper file", e);
        }

        return bytes;
    }

    private byte[] generatePDF(JasperPrint jasperPrint) throws JRException {
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private byte[] generateExcel(JasperPrint jasperPrint) throws JRException {
        byte[] bytes = null;
        SimpleExporterInput input = new SimpleExporterInput(jasperPrint);
        try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream()) {
            SimpleOutputStreamExporterOutput output = new SimpleOutputStreamExporterOutput(byteArray);
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(input);
            exporter.setExporterOutput(output);
            exporter.exportReport();
            bytes = byteArray.toByteArray();
            output.close();

        } catch (IOException e) {
            log.error("IO error encountered", e);
        }
        return bytes;
    }

    public void generateReport(ReportData reportData, HttpServletResponse response) throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        JasperPrint jasperPrint = null;
        try {
            ExportFormat format = reportData.getFormat();
            if (format == null) {
                format = ExportFormat.XLSX;
            }
            List dataList = reportData.getData();
            String inputFileName = reportData.getReportName();
            JasperReport jasperReport = null;
            HashMap param = reportConfig();
            InputStream reportInputStream = resourceLoader.getResource(appProperties.getReportLoc() +inputFileName +".jasper").getInputStream();
            // Check if a compiled report exists
            if (reportInputStream != null) {
                jasperReport = (JasperReport) JRLoader.loadObject(reportInputStream);
            } // Compile report from source and save
            else {
                String jrxml = storageService.loadJrxmlFile(inputFileName + ".jrxml");
                log.info("{} loaded. Compiling report", jrxml);
                jasperReport = JasperCompileManager.compileReport(jrxml);
            }
            // Get your data source
            JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(dataList,false);
            // Add parameters
            param.putAll(param);
            // Fill the report
            if(dataList.isEmpty())
                jasperPrint = JasperFillManager.fillReport(jasperReport, param, conn);
            else
                jasperPrint = JasperFillManager.fillReport(jasperReport, param, jrBeanCollectionDataSource);

            export(jasperPrint, format, inputFileName, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void export(final JasperPrint jprint, ExportFormat type, String reportName, HttpServletResponse response) throws JRException, IOException {
        final Exporter exporter;
        final OutputStream out = response.getOutputStream();
        SimpleOutputStreamExporterOutput exporterOutput = null;
        boolean html = false;

        switch (type.name()) {
            case "HTML":
                exporter = new HtmlExporter();
                response.setContentType("text/html");
                exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));
                response.setHeader("Content-Disposition", String.format("attachment; filename=" + reportName + "." + type.name().toLowerCase()));
                break;

            case "CSV":
                exporter = new JRCsvExporter();
                exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", String.format("attachment; filename=" + reportName + "." + type.name().toLowerCase()));
                break;

            case "XML":
                exporter = new JRXmlExporter();
                response.setContentType("text/xml");
                response.setHeader("Content-Disposition", String.format("attachment; filename=" + reportName + "." + type.name().toLowerCase()));
                break;

            case "XLS":
            case "XLSX":
                exporter = new JRXlsxExporter();
                AbstractXlsReportConfiguration config = new SimpleXlsxReportConfiguration();
                config.setOnePagePerSheet(false);
//                config.setDetectCellType(Boolean.TRUE);
                config.setRemoveEmptySpaceBetweenRows(Boolean.TRUE);
                config.setCollapseRowSpan(Boolean.TRUE);
                config.setIgnoreGraphics(Boolean.TRUE);
                config.setWrapText(Boolean.TRUE);
                config.setColumnWidthRatio(2.0F);
                config.setShowGridLines(Boolean.FALSE);
                config.setRemoveEmptySpaceBetweenColumns(Boolean.TRUE);
                config.setFontSizeFixEnabled(false);
                config.setSheetNames(new String[]{"Sheet1"});
                exporter.setConfiguration(config);
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", String.format("attachment; filename=" + reportName + "." + type.name().toLowerCase()));

                break;

            case "PDF":
                exporter = new JRPdfExporter();
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", String.format("attachment; filename=" + reportName + "." + type.name().toLowerCase()));
                break;

            default:
                throw new JRException("Unknown report format: " + type);
        }

        if (!type.name().equalsIgnoreCase("HTML") && !type.name().equalsIgnoreCase("CSV")) {
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        }
        exporterOutput = new SimpleOutputStreamExporterOutput(out);
        SimpleExporterInput exporterInput = new SimpleExporterInput(jprint);
        exporter.setExporterInput(exporterInput);
        exporter.exportReport();
    }

    /**
     *
     */
    
    private HashMap reportConfig() {
        HashMap jasperParameter = new HashMap();

        Facility facility = facilityService.loggedFacility();

        jasperParameter.put("facilityName", facility.getFacilityName());
        jasperParameter.put("facilityType", facility.getFacilityType());
        jasperParameter.put("logo", facility.getLogo());
        jasperParameter.put("orgLegalName", facility.getOrganization().getLegalName());
        jasperParameter.put("orgName", facility.getOrganization().getOrganizationName());
        jasperParameter.put("TaxNumber", facility.getOrganization().getTaxNumber());
        jasperParameter.put("orgWebsite", facility.getOrganization().getWebsite());
        if (!facility.getOrganization().getAddress().isEmpty()) {
            jasperParameter.put("orgAddressCountry", facility.getOrganization().getAddress().get(0).getCountry());
            jasperParameter.put("orgAddressCounty", facility.getOrganization().getAddress().get(0).getCounty());
            jasperParameter.put("orgAddressLine1", facility.getOrganization().getAddress().get(0).getLine1());
            jasperParameter.put("orgAddressLine2", facility.getOrganization().getAddress().get(0).getLine2());
            jasperParameter.put("orgPostalCode", facility.getOrganization().getAddress().get(0).getPostalCode());
            jasperParameter.put("orgTown", facility.getOrganization().getAddress().get(0).getTown());
            jasperParameter.put("orgType", facility.getOrganization().getAddress().get(0).getType());
        }

        if (!facility.getOrganization().getContact().isEmpty()) {
            jasperParameter.put("contactEmail", facility.getOrganization().getContact().get(0).getEmail());
            jasperParameter.put("contactFullName", facility.getOrganization().getContact().get(0).getFullName());
            jasperParameter.put("contactMobile", facility.getOrganization().getContact().get(0).getMobile());
            jasperParameter.put("salutation", facility.getOrganization().getContact().get(0).getSalutation());
            jasperParameter.put("telephone", facility.getOrganization().getContact().get(0).getTelephone());
        }
        
         if(facility.getLogo()==null)
             jasperParameter.put("IMAGE_DIR", appProperties.getReportLoc()+"/logo.png");
         else
             jasperParameter.put("IMAGE", facility.getLogo());
        return jasperParameter;
    }

}