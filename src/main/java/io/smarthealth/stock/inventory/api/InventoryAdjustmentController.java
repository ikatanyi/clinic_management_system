package io.smarthealth.stock.inventory.api;

import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.smarthealth.stock.inventory.data.AdjustmentData;
import io.smarthealth.stock.inventory.data.StockAdjustmentData;
import io.smarthealth.stock.inventory.data.TransData;
import io.smarthealth.stock.inventory.service.InventoryAdjustmentService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * @author Kelsas
 */
@RestController
@Slf4j
@Api
@RequiredArgsConstructor
@RequestMapping("/api")
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService service;
    private final AuditTrailService auditTrailService;

    @PostMapping("/inventory-adjustment")
    @PreAuthorize("hasAuthority('create_inventoryadjustment')")
    public ResponseEntity<?> createStockAdjustment(@Valid @RequestBody AdjustmentData data) {

        String result = service.createStockAdjustment(data);

        Pager<TransData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Stock Adjustment successful");
        pagers.setContent(new TransData(result));
        auditTrailService.saveAuditTrail("Inventory", "Created an inventory adjustment");
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);

    }

    @GetMapping("/inventory-adjustment/{id}")
    @PreAuthorize("hasAuthority('view_inventoryadjustment')")
    public StockAdjustmentData searchStockAdjustment(@PathVariable(value = "id") Long id) {
        StockAdjustmentData stocks = service.getStockAdjustment(id).toData();
        auditTrailService.saveAuditTrail("Inventory", "Searched for an inventory adjustment with id "+id);
        return stocks;
    }
 
    @GetMapping("/inventory-adjustment")
    @PreAuthorize("hasAuthority('view_inventoryadjustment')")
    public ResponseEntity<?> getStockAdjustment(
            @RequestParam(value = "store_id", required = false) final Long store,
            @RequestParam(value = "item_id", required = false) final Long item,
            @RequestParam(value = "dateRange", required = false) String dateRange, //2020-01-01..2020-01-24
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);
        DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);
        
        Page<StockAdjustmentData> list = service.getStockAdjustments(store, item, range, pageable);

        Pager<List<StockAdjustmentData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Stock Adjustment ");
        pagers.setPageDetails(details);
        auditTrailService.saveAuditTrail("Inventory", "Viewed all inventory adjustments");
        return ResponseEntity.ok(pagers);
    }

}
