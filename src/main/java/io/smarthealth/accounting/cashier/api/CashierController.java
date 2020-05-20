package io.smarthealth.accounting.cashier.api;

import io.smarthealth.accounting.cashier.data.CashierData;
import io.smarthealth.accounting.cashier.data.CashierShift;
import io.smarthealth.accounting.cashier.data.ShiftCommand;
import io.smarthealth.accounting.cashier.data.ShiftData;
import io.smarthealth.accounting.cashier.domain.Cashier;
import io.smarthealth.accounting.cashier.domain.Shift;
import io.smarthealth.accounting.cashier.domain.ShiftStatus;
import io.smarthealth.accounting.cashier.service.CashierService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
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
@RequestMapping("/api")
public class CashierController {

    private final CashierService service;

    public CashierController(CashierService service) {
        this.service = service;
    }

    @PostMapping("/cashiers")
    @PreAuthorize("hasAuthority('create_cashiers')") 
    public ResponseEntity<?> createCashier(@Valid @RequestBody CashierData cashierData) {
        Cashier result = service.createCashier(cashierData);
        return ResponseEntity.status(HttpStatus.CREATED).body(result.toData());
    }

    @GetMapping("/cashiers/{id}")
    @PreAuthorize("hasAuthority('view_cashiers')") 
    public ResponseEntity<?> getCashier(@PathVariable(value = "id") Long code) {
        Cashier result = service.getCashier(code);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.toData());
    }

    @PostMapping("/cashiers/{id}/shift")
    @PreAuthorize("hasAuthority('create_cashiersShift')") 
    public ResponseEntity<?> shiftCommands(@PathVariable(value = "id") Long code, @RequestParam("status") ShiftCommand status) {
        Cashier result = service.getCashier(code);
        Shift shift;
        if (status == ShiftCommand.Start) {
            shift = service.startShift(result);
        } else {
            shift = service.closeShift(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(shift.toData());
    }

    @GetMapping("/cashiers/{id}/shift")
    @PreAuthorize("hasAuthority('view_cashiersShift')") 
    public ResponseEntity<?> getCashierShift(@PathVariable(value = "id") Long code, @RequestParam(value = "status", required = false) ShiftStatus status) {
        Cashier result = service.getCashier(code);
        List<Shift> shifts = service.getShiftsByCashier(result, status);
        List<ShiftData> shiftData = shifts.stream()
                .map(x -> x.toData())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(shiftData);
    }

    @GetMapping("/cashiers/{id}/shift/{shiftNo}/status")
    @PreAuthorize("hasAuthority('view_cashiersShift')") 
    public ResponseEntity<?> getCashierShiftStatus(@PathVariable(value = "id") Long code, @PathVariable(value = "shiftNo") String shiftNo) {
        Cashier cashier = service.getCashier(code);
        Shift shift = service.findByCashierAndShiftNo(cashier, shiftNo);

        return ResponseEntity.ok("{ \"status\": \"" + shift.getStatus() + "\" }\n");
    }

    @PutMapping("/cashiers/{id}/shift/{shiftNo}/close")
    @PreAuthorize("hasAuthority('edit_cashiersShift')") 
    public ResponseEntity<?> endCashierShift(@PathVariable(value = "id") Long code, @PathVariable(value = "shiftNo") String shiftNo) {

        //check if this shift exist
        Shift shift = service.closeShift(code, shiftNo);

        return ResponseEntity.ok(shift.toData());
    }

    @PutMapping("/cashiers/{id}")
    @PreAuthorize("hasAuthority('edit_cashiers')") 
    public ResponseEntity<?> updateCashier(@PathVariable(value = "id") Long id, Cashier data) {
        Cashier cashDrawer = service.updateCashier(id, data);
        return ResponseEntity.ok(cashDrawer);
    }

    @GetMapping("/cashiers")
    @PreAuthorize("hasAuthority('view_cashiers')") 
    public ResponseEntity<?> getAllCashiers(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);

        Page<CashierData> list = service.fetchAllCashiers(pageable)
                .map(x -> x.toData());

        Pager<List<CashierData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Cash Drawers");
        pagers.setPageDetails(details);

        return ResponseEntity.ok(pagers);
    }

    @GetMapping("/cashiers/shifts")
    @PreAuthorize("hasAuthority('view_cashiersShifts')") 
    public ResponseEntity<?> getAllCashierShifts(
            @RequestParam(value = "showBalance", required = false) Boolean showBalance,
            @RequestParam(value = "status", required = false) ShiftStatus status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {
        Pageable pageable = PaginationUtil.createPage(page, size);

        if (showBalance != null && showBalance) {
            Page<CashierShift> list = service.getCashierShiftWithBalance(status, pageable);
            Pager<List<CashierShift>> pagers = new Pager();
            pagers.setCode("0");
            pagers.setMessage("Success");
            pagers.setContent(list.getContent());

            PageDetails details = new PageDetails();
            details.setPage(list.getNumber() + 1);
            details.setPerPage(list.getSize());
            details.setTotalElements(list.getTotalElements());
            details.setTotalPage(list.getTotalPages());
            details.setReportName("Cashier Shifts");
            pagers.setPageDetails(details);
            return ResponseEntity.ok(pagers);
        }

        Page<ShiftData> list = service.fetchAllShifts(status, pageable)
                .map(x -> x.toData());

        Pager<List<ShiftData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());

        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Cashier Shifts");
        pagers.setPageDetails(details);

        return ResponseEntity.ok(pagers);
    }

}
