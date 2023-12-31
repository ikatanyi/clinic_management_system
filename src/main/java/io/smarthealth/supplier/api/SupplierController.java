package io.smarthealth.supplier.api;

import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.smarthealth.supplier.data.SupplierBalance;
import io.smarthealth.supplier.data.SupplierBalanceAging;
import io.smarthealth.supplier.data.SupplierData;
import io.smarthealth.supplier.data.SupplierStatement;
import io.smarthealth.supplier.domain.Supplier;
import io.smarthealth.supplier.domain.SupplierMetadata;
import io.smarthealth.supplier.service.SupplierService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Kelsas
 */
@RestController
@Slf4j
@Api
@RequestMapping("/api")
public class SupplierController {

    private final SupplierService service;
    private final AuditTrailService auditTrailService;

    public SupplierController(SupplierService supplierService, AuditTrailService auditTrailService) {
        this.service = supplierService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('create_suppliers')")
    public ResponseEntity<?> createSupplier(@Valid @RequestBody SupplierData supplierData) {

        if (service.getSupplierByName(supplierData.getSupplierName(), supplierData.getLegalName()).isPresent()) {
            throw APIException.conflict("Supplier with name {0} already exists.", supplierData.getSupplierName());
        }

        SupplierData result = service.createSupplier(supplierData);

        Pager<SupplierData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Supplier created successful");
        pagers.setContent(result);
        auditTrailService.saveAuditTrail("Suppliers", "Created a supplier " + result.getSupplierName());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);

    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('create_suppliers')")
    public ResponseEntity<?> updateSupplier(@PathVariable(value = "id") Long id, @Valid @RequestBody SupplierData supplierData) {

        SupplierData result = service.updateSupplier(id, supplierData);
        Pager<SupplierData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Supplier created successful");
        pagers.setContent(result);
        auditTrailService.saveAuditTrail("Suppliers", "Created a supplier " + result.getSupplierName());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);

    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('view_suppliers')")
    public SupplierData getSupplier(@PathVariable(value = "id") Long code) {
        Supplier supplier = service.getSupplierById(code)
                .orElseThrow(() -> APIException.notFound("Supplier with id {0} not found.", code));
        auditTrailService.saveAuditTrail("Suppliers", "Created a supplier " + supplier.getSupplierName());
        return supplier.toData();
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('view_suppliers')")
    public ResponseEntity<?> getAllSuppliers(
            @RequestParam(value = "includeClosed", required = false) Boolean includeClosed,
            @RequestParam(value = "q", required = false) final String term,
            @RequestParam(value = "type", required = false) final String type,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);

        Page<SupplierData> list = service.getSuppliers(type, includeClosed, term, pageable).map(u -> u.toData());

        Pager<List<SupplierData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Suppliers");
        pagers.setPageDetails(details);
        auditTrailService.saveAuditTrail("Suppliers", "Viewed all suppliers ");
        return ResponseEntity.ok(pagers);
    }

    //generate a single api that returns all the setup required for this object
    @GetMapping("/suppliers/$metadata")
    @PreAuthorize("hasAuthority('view_suppliers')")
    public ResponseEntity<?> getSupplierMetadata() {
        SupplierMetadata metadata = service.getSupplierMetadata();
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/suppliers/{supplierId}/statement")
    public ResponseEntity<?> getStatement(
            @PathVariable(value = "supplierId") Long supplierId,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);

        List<SupplierStatement> list = service.getStatement(supplierId, range);

        return ResponseEntity.ok(PaginationUtil.paginateList(list, "Supplier Statement", "", pageable));
    }

    @GetMapping("/suppliers/balance")
    public ResponseEntity<Pager<SupplierBalance>> getSupplierBalances(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
        Page<SupplierBalance> list = service.getSupplierBalances(pageable);
        Pager<SupplierBalance> balances = (Pager<SupplierBalance>) PaginationUtil.toPager(list, "Supplier Balance");
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/suppliers/balance/aging")
    public ResponseEntity<Pager<SupplierBalanceAging>> getSupplierAgingBalance(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
        Page<SupplierBalanceAging> list = service.getSupplierAgingBalances(pageable);
        Pager<SupplierBalanceAging> balances = (Pager<SupplierBalanceAging>) PaginationUtil.toPager(list, "Supplier Balance");
        return ResponseEntity.ok(balances);
    }
}
