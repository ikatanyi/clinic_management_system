package io.smarthealth.stat.service;

import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.smarthealth.report.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JasperReportsManager implements ReportService {
    private final StorageService storageService;


    @Override
    public byte[] generateReport(ExportFormat format, String inputFileName, Map<String, Object> params) {
        return generateReport(format, inputFileName, params, new JREmptyDataSource());
    }

    @Override
    public byte[] generateReport(ExportFormat format, String inputFileName, Map<String, Object> params, JRDataSource dataSource) {
        byte[] bytes = null;
        JasperReport jasperReport = null;
        try {
            // Check if a compiled report exists
            if (storageService.jasperFileExists(inputFileName)) {
                jasperReport = (JasperReport) JRLoader.loadObject(storageService.loadJasperFile(inputFileName));
            }
            // Compile report from source and save
            else {
                String jrxml = storageService.loadJrxmlFile(inputFileName);
                log.info("{} loaded. Compiling report", jrxml);
                jasperReport = JasperCompileManager.compileReport(jrxml);
                // Save compiled report. Compiled report is loaded next time
                JRSaver.saveObject(jasperReport, storageService.loadJasperFile(inputFileName));
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            if (format == ExportFormat.PDF)
                bytes = generatePDF(jasperPrint);
            else if (format == ExportFormat.XLSX)
                bytes = generateExcel(jasperPrint);
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
}
