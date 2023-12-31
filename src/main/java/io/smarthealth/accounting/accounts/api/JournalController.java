package io.smarthealth.accounting.accounts.api;

import io.smarthealth.accounting.accounts.data.JournalEntryData;
import io.smarthealth.accounting.accounts.domain.JournalEntry;
import io.smarthealth.accounting.accounts.domain.JournalReversal;
import io.smarthealth.accounting.accounts.domain.JournalState;
import io.smarthealth.accounting.accounts.domain.TransactionType;
import io.smarthealth.accounting.accounts.service.JournalService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Api
@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final AuditTrailService auditTrailService;

//    public JournalController(JournalService journalService) {
//        this.journalService = journalService;
//    }
    //CRUD

    @PostMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('create_journal')")
    public ResponseEntity<?> createJournalEntry(@RequestBody @Valid final JournalEntryData journalEntry) {

        if (journalEntry.getDebtors().isEmpty()) {
            throw APIException.badRequest("Debtors must be given.");
        }
        if (journalEntry.getCreditors().isEmpty()) {
            throw APIException.badRequest("Creditors must be given.");
        }

        JournalEntry result = journalService.createJournal(journalEntry);

        Pager<JournalEntryData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Journal created successful");
        pagers.setContent(result.toData());
        auditTrailService.saveAuditTrail("Journal", "Created Journal "+result.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('view_journal')")
    public ResponseEntity<?> fetchJournalEntries(
            @RequestParam(value = "transaction_no", required = false) final String transactionNo,
            @RequestParam(value = "transaction_type", required = false) final TransactionType type,
            @RequestParam(value = "status", required = false) final JournalState status,
            @RequestParam(value = "dateRange", required = false) final String dateRange,
            @RequestParam(value = "account_no", required = false) final String accountNo,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        final DateRange range = DateRange.fromIsoStringOrReturnNull(dateRange);

        Pageable pageable = PaginationUtil.createPage(page, size, Sort.by("date").descending());
        Page<JournalEntryData> list = journalService.findJournals(transactionNo, type, status, range, accountNo, pageable);

        Pager<List<JournalEntryData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Journal Entries");
        pagers.setPageDetails(details);
        auditTrailService.saveAuditTrail("Journal", "View Journals ");
        return ResponseEntity.ok(pagers);
    }

    @GetMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('view_journal')")
    ResponseEntity<JournalEntryData> findJournalEntry(@PathVariable("id") Long id) {
        JournalEntry optionalJournalEntry = journalService.findJournalIdOrThrow(id);
        auditTrailService.saveAuditTrail("Journal", "Viewed Journal "+optionalJournalEntry.getDescription());
        return ResponseEntity.ok(optionalJournalEntry.toData());
    }

    @PostMapping("/{id}/reverse")
    @ResponseBody
    @PreAuthorize("hasAuthority('view_journal')")
    ResponseEntity<JournalEntryData> reverseJournalEntry(@PathVariable("id") Long id, @RequestBody @Valid JournalReversal journalReversal) {
        JournalEntry optionalJournalEntry = journalService.reverseJournal(id, journalReversal);
        auditTrailService.saveAuditTrail("Journal", "Reversed Journal Entry for"+optionalJournalEntry.getDescription());
        return ResponseEntity.ok(optionalJournalEntry.toData());
    }

}
