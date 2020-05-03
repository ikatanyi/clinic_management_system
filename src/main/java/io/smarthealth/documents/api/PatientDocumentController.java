/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.documents.api;

import io.smarthealth.documents.data.DocumentData;
import io.smarthealth.documents.domain.enumeration.DocumentType;
import io.smarthealth.documents.domain.enumeration.Status;
import io.smarthealth.documents.service.FileStorageService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.imports.service.UploadService;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Kennedy.Imbenzi
 */
@RestController
@RequestMapping("/api")
@Api(value = "Patient-Document-Controller", description = "upload and download of documents")
public class PatientDocumentController {

    @Autowired
    FileStorageService fileService;

    @Autowired
    UploadService uploadService;

    @PostMapping("/patient-document/batch")
    public @ResponseBody
    ResponseEntity<?> batchUpload(@ModelAttribute @Valid final List<DocumentData> documentData) {
        List<DocumentData> documentDataArr = fileService.batchDocumentUpload(documentData)
                .stream()
                .map((document) -> document.toData()
                ).collect(Collectors.toList());

        Pager<List<DocumentData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(documentDataArr);
        PageDetails details = new PageDetails();
        details.setReportName("Documents uploaded successfully");
        pagers.setPageDetails(details);
        return ResponseEntity.status(HttpStatus.OK).body(pagers);
    }

    @GetMapping("/patient-document/{id}")
    public ResponseEntity<?> fetchServiceTemplate(@PathVariable("id") final Long id) {
        DocumentData documentData = fileService.getDocumentByIdWithFailDetection(id).toData();
        return ResponseEntity.ok(documentData);

    }

    @GetMapping("/patient-document")
    public ResponseEntity<?> Documents(
            @RequestParam(value = "patientNumber", required = false) String patientNumber,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "servicePointId", required = false) Long servicePointId,
            @RequestParam(value = "pag", required = false) Integer pag,
            @RequestParam(value = "pageSize", required = false) Integer size
    ) {
        final DateRange range = DateRange.fromIsoString(dateRange);
        Pageable pageable = PaginationUtil.createPage(pag, size);
        List<DocumentData> testData = fileService.findAllDocuments(patientNumber, documentType, status, servicePointId, range, pageable)
                .stream()
                .map((template) -> template.toData()
                ).collect(Collectors.toList());

        Pager page = new Pager();
        page.setCode("200");
        page.setContent(testData);
        page.setMessage("Service Templates fetched successfully");
        PageDetails details = new PageDetails();
        details.setPage(1);
        details.setPerPage(25);
        details.setReportName("Service Template fetched");
//        details.setTotalElements(Long.parseLong(String.valueOf(pag.getNumberOfElements())));
        page.setPageDetails(details);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/patient-document")
    public @ResponseBody
    ResponseEntity<?> Upload(@ModelAttribute @Valid final DocumentData documentData) {
        DocumentData savedDocumentData = fileService.documentUpload(documentData).toData();

        Pager<DocumentData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(savedDocumentData);
        PageDetails details = new PageDetails();
        details.setReportName("Document");
        pagers.setPageDetails(details);
        return ResponseEntity.status(HttpStatus.OK).body(pagers);
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) throws IOException {
        // Load file as Resource
        Resource resource = uploadService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}