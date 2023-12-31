package io.smarthealth.debtor.claim.dispatch.api;

import io.smarthealth.debtor.claim.dispatch.data.DispatchData;
import io.smarthealth.debtor.claim.dispatch.service.DispatchService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 *
 * @author Kennedy.Ikatanyi
 */
@Api
@RestController
@RequestMapping("/api/")
public class DispatchController {

    private final DispatchService dispatchService;
    private final AuditTrailService auditTrailService;

    public DispatchController(DispatchService dispatchService, AuditTrailService auditTrailService) {
        this.dispatchService = dispatchService;
        this.auditTrailService = auditTrailService;
    }  

    
    @PostMapping("/dispatch")
    @PreAuthorize("hasAuthority('create_dispatch')")
    public ResponseEntity<?> createDispatch(@Valid @RequestBody DispatchData dispatchData) {

        DispatchData remittance = dispatchService.map(dispatchService.createDispatch(dispatchData));

        Pager<DispatchData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Dispatch successfully Created.");
        pagers.setContent(remittance);
        auditTrailService.saveAuditTrail("Dispatch", "Created a dispatch note dispatchNo "+remittance.getDispatchNo());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping("/dispatch/{id}")
    @PreAuthorize("hasAuthority('view_dispatch')")
    public DispatchData getDispatch(@PathVariable(value = "id") Long id) {
        DispatchData dispatch = DispatchData.map(dispatchService.getDispatchByIdWithFailDetection(id));
        auditTrailService.saveAuditTrail("Dispatch", "Viewed a dispatch note dispatchNo "+dispatch.getDispatchNo());
        return dispatch;
    }

    @PutMapping("/dispatch/{id}")
    @PreAuthorize("hasAuthority('edit_dispatch')")
    public DispatchData updateRemitance(@PathVariable(value = "id") Long id, DispatchData dispatchData) {
        DispatchData dispatch = DispatchData.map(dispatchService.updateDispatch(id, dispatchData));
        auditTrailService.saveAuditTrail("Dispatch", "Edited a dispatch note dispatchNo "+dispatch.getDispatchNo());
        return dispatch;
    }

    @GetMapping("/dispatch")
    @PreAuthorize("hasAuthority('view_dispatch')")
    public ResponseEntity<?> getAllDispatches(
            @RequestParam(value = "payerId", required = false) Long payerId,      
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);
        Page<DispatchData> list = dispatchService.getAllDispatches(payerId, range, pageable)
                .map(disp -> dispatchService.map(disp));
        auditTrailService.saveAuditTrail("Dispatch", "Viewd all dispatch notes");
        Pager<List<DispatchData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Dispatches");
        pagers.setPageDetails(details);
        return ResponseEntity.ok(pagers);
    }
}
