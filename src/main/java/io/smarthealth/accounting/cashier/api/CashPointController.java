package io.smarthealth.accounting.cashier.api;

import io.smarthealth.accounting.cashier.data.CashPointData;
import io.smarthealth.accounting.cashier.domain.CashPoint;
import io.smarthealth.accounting.cashier.service.CashPointService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class CashPointController {

    private final CashPointService service;
    private final AuditTrailService auditTrailService;

//    public CashPointController(CashPointService service) {
//        this.service = service;
//    }
 
    @PostMapping("/cashpoints")
    @PreAuthorize("hasAuthority('create_cashPoints')") 
    public ResponseEntity<?> createCashPoint(@Valid @RequestBody CashPointData cashPoint) {

        CashPoint result = service.createCashPoint(cashPoint);

        Pager<CashPoint> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("CashPoint Success Created");
        pagers.setContent(result);
        auditTrailService.saveAuditTrail("CashPoint", "created cash point "+result.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping("/cashpoints/{id}")
    @PreAuthorize("hasAuthority('view_cashPoints')") 
    public ResponseEntity<?> getCashPoint(@PathVariable(value = "id") Long code) {
        CashPoint result = service.getCashPoint(code);
        Pager<CashPointData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("CashPoint Success updated");
        pagers.setContent(result.toData());
        auditTrailService.saveAuditTrail("CashPoint", "Viewed cash point "+result.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pagers);
    }

    @PutMapping("/cashpoints/{id}")
    @PreAuthorize("hasAuthority('edit_cashPoints')") 
    public ResponseEntity<?> updateCashPoint(@PathVariable(value = "id") Long id, @Valid @RequestBody CashPointData data) {
        CashPoint cashPoint = service.updateCashPoint(id, data);
        auditTrailService.saveAuditTrail("CashPoint", "edited cash point "+cashPoint.getName());
        return ResponseEntity.ok(cashPoint.toData());
    }

    @GetMapping("/cashpoints")
    @PreAuthorize("hasAuthority('view_cashPoints')") 
    public ResponseEntity<?> getAllCashPointes(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
       auditTrailService.saveAuditTrail("CashPoint", "viewed all cash points ");
        Page<CashPointData> list = service.fetchAllCashPoints(pageable).map(x -> x.toData());
        Pager<List<CashPointData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Cash Points");
        pagers.setPageDetails(details);

        return ResponseEntity.ok(pagers);
    }
}
