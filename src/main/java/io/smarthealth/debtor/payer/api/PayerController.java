package io.smarthealth.debtor.payer.api;

import io.smarthealth.accounting.account.domain.Account;
import io.smarthealth.accounting.account.service.AccountService;
import io.smarthealth.accounting.pricebook.domain.PriceBook;
import io.smarthealth.accounting.pricebook.service.PricebookService;
import io.smarthealth.administration.app.domain.BankBranch;
import io.smarthealth.administration.app.domain.PaymentTerms;
import io.smarthealth.administration.app.service.AdminService;
import io.smarthealth.administration.app.service.PaymentTermsService;
import io.smarthealth.debtor.payer.data.PayerData;
import io.smarthealth.debtor.payer.domain.Payer;
import io.smarthealth.debtor.payer.service.PayerService;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.swagger.annotations.Api;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 *
 * @author Simon.Waweru
 */
@Api
@RestController
@RequestMapping("/api")
public class PayerController {
    
    private final PayerService payerService;
    private final AdminService adminService;
    private final AccountService accountService;
    private final PaymentTermsService paymentTermsService;
    private final PricebookService pricebookService;
    
    public PayerController(PayerService payerService, AdminService adminService, AccountService accountService, PaymentTermsService paymentTermsService, PricebookService pricebookService) {
        this.payerService = payerService;
        this.adminService = adminService;
        this.accountService = accountService;
        this.paymentTermsService = paymentTermsService;
        this.pricebookService = pricebookService;
    }
    
    @PostMapping("/payer")
    public ResponseEntity<?> createPayer(@Valid @RequestBody PayerData payerData) {
        
        Payer payer = PayerData.map(payerData);
        BankBranch bankBranch = adminService.fetchBankBranchById(payerData.getBranchId());
        Account debitAccount = accountService.findOneWithNotFoundDetection(payerData.getDebitAccountNo());
        PaymentTerms paymentTerms = paymentTermsService.getPaymentTermByIdWithFailDetection(payerData.getPaymentTermId());
        PriceBook priceBook = pricebookService.getPricebookWithNotFoundExeption(payerData.getPayerId());
        payer.setBankBranch(bankBranch);
        payer.setDebitAccount(debitAccount);
        payer.setPaymentTerms(paymentTerms);
        payer.setPriceBook(priceBook);
        Payer result = payerService.createPayer(payer);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/payer/{id}")
                .buildAndExpand(result.getId()).toUri();
        
        PayerData data = PayerData.map(result);
        
        return ResponseEntity.created(location).body(data);
    }
    
    @GetMapping("payer")
    public ResponseEntity<?> fetchAllPayers(Pageable pageable) {
        Page<PayerData> payers = payerService.fetchPayers(pageable).map(p -> PayerData.map(p));
        
        Pager<List<PayerData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(payers.getContent());
        PageDetails details = new PageDetails();
        details.setPage(payers.getNumber() + 1);
        details.setPerPage(payers.getSize());
        details.setTotalElements(payers.getTotalElements());
        details.setTotalPage(payers.getTotalPages());
        details.setReportName("Payer List");
        pagers.setPageDetails(details);
        
        return ResponseEntity.ok(pagers);
    }
    
}
