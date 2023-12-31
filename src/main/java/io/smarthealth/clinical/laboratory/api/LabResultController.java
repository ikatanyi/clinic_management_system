package io.smarthealth.clinical.laboratory.api;

import io.smarthealth.clinical.laboratory.data.LabResultData;
import io.smarthealth.clinical.laboratory.data.ValidateResultData;
import io.smarthealth.clinical.laboratory.domain.LabResult;
import io.smarthealth.clinical.laboratory.service.LaboratoryService;
import io.smarthealth.documents.data.DocResponse;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@RequestMapping("/api")
public class LabResultController {

    private final LaboratoryService service;
    private final AuditTrailService auditTrailService;

    public LabResultController(LaboratoryService service, AuditTrailService auditTrailService) {
        this.service = service;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/labs/results")
    @PreAuthorize("hasAuthority('create_labresults')")
    public ResponseEntity<?> createLabResult(@Valid @RequestBody LabResultData data) {
        LabResult results = service.createLabResult(data);
        auditTrailService.saveAuditTrail("Laboratory", "Entered lab results for "+results.getAnalyte()+" identified by labNumber "+results.getLabNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(results.toData());
    }

    @PostMapping("/labs/results/batch")
    @PreAuthorize("hasAuthority('create_labresults')")
    public ResponseEntity<?> createLabResult(@Valid @RequestBody List<LabResultData> data) {

        List<LabResultData> lists = service.createLabResult(data)
                .stream()
                .map(x -> {
                    auditTrailService.saveAuditTrail("Laboratory", "Entered lab results for "+x.getAnalyte()+" identified by labNumber "+x.getLabNumber());
                    return x.toData();
                        })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(lists);
    }

    @GetMapping("/labs/results/{id}")
    @PreAuthorize("hasAuthority('view_labresults')")
    public ResponseEntity<?> getLabResult(@PathVariable(value = "id") Long id) {
        LabResult request = service.getResult(id);
        auditTrailService.saveAuditTrail("Laboratory", "viewed lab results for "+request.getAnalyte()+" identified by labNumber "+request.getLabNumber());
        return ResponseEntity.ok(request.toData());
    }

    @PutMapping("/labs/results/{id}")
    @PreAuthorize("hasAuthority('edit_labresults')")
    public ResponseEntity<?> updateLabResult(@PathVariable(value = "id") Long id, @Valid @RequestBody LabResultData data) {
        LabResult item = service.updateLabResult(id, data);
        auditTrailService.saveAuditTrail("Laboratory", "Edited lab results for "+item.getAnalyte()+" identified by labNumber "+item.getLabNumber());
        return ResponseEntity.ok(item.toData());
    }

    @DeleteMapping("/labs/results/{id}")
    @PreAuthorize("hasAuthority('delete_labresults')")
    public ResponseEntity<?> deleteLabResult(@PathVariable(value = "id") Long id) {
        service.voidLabResult(id);
        return ResponseEntity.accepted().build();
    }
    //String visitNumber, String patientNumber, String labNumber, Boolean walkin, String testName, DateRange range, Pageable page

    @GetMapping("/labs/results")
    @PreAuthorize("hasAuthority('view_labresults')")
    public ResponseEntity<?> getLabResults(
            @RequestParam(value = "visit_no", required = false) String visitNumber,
            @RequestParam(value = "patient_no", required = false) String patientNumber,
            @RequestParam(value = "lab_no", required = false) String labNumber,
            @RequestParam(value = "is_walkin", required = false) Boolean walkin,
            @RequestParam(value = "test_name", required = false) String testName,
            @RequestParam(value = "order_no", required = false) String orderNo,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        final DateRange range = DateRange.fromIsoString(dateRange);
        Pageable pageable = PaginationUtil.createPage(page, size);
        Page<LabResultData> list = service.getLabResults(visitNumber, patientNumber, labNumber, walkin, testName, orderNo, range, pageable)
                .map(x ->{
                    auditTrailService.saveAuditTrail("Laboratory", "viewed lab results for "+x.getAnalyte()+" identified by labNumber "+x.getLabNumber());
                    return x.toData();
                        });

        Pager<List<LabResultData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Lab Results list");
        pagers.setPageDetails(details);
        return ResponseEntity.ok(pagers);
    }

    //upload the file to server
    @PostMapping("/labs/results/{id}/upload")
//    @PreAuthorize("hasAuthority('view_labresults')")
    public ResponseEntity<?> uploadResults(
            @PathVariable(value = "id") Long testId,
            @RequestParam(required = false) String name,
            @RequestParam MultipartFile file
    ) {

        DocResponse doc = service.uploadDocument(testId, name, file);
        auditTrailService.saveAuditTrail("Laboratory", "Uploaded lab test identified by test Id "+testId);
        return ResponseEntity.status(HttpStatus.OK).body(doc);
    }
    @PutMapping("/labs/results/validate")
    public ResponseEntity<List<LabResultData>> validateLabResult(@Valid @RequestBody List<ValidateResultData> data) {
        List<LabResultData> lists = service.validateResults(data).stream()
                .map(LabResult::toData)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lists);
    }
    }
