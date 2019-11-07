package io.smarthealth.accounting.account.service;

import io.smarthealth.accounting.account.data.Credit;
import io.smarthealth.accounting.account.data.Debit;
import io.smarthealth.accounting.account.data.JournalData;
import io.smarthealth.accounting.account.domain.Account;
import io.smarthealth.accounting.account.domain.AccountRepository;
import io.smarthealth.accounting.account.domain.Journal;
import io.smarthealth.accounting.account.domain.JournalEntry;
import io.smarthealth.accounting.account.domain.JournalRepository;
import io.smarthealth.accounting.account.domain.enumeration.JournalState;
import io.smarthealth.accounting.account.domain.specification.JournalSpecification;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.sequence.SequenceService;
import io.smarthealth.infrastructure.sequence.SequenceType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 *
 * @author Kelsas
 */
@Service
public class JournalService {

    private final JournalRepository journalRepository;
     private final JournalBalanceUpdateService balanceUpdateService;
    private final AccountService accountService;
    private final SequenceService sequenceService;

    public JournalService(JournalRepository journalRepository, JournalBalanceUpdateService balanceUpdateService, AccountService accountService, SequenceService sequenceService) {
        this.journalRepository = journalRepository;
        this.balanceUpdateService = balanceUpdateService;
        this.accountService = accountService;
        this.sequenceService = sequenceService;
    }
 
    public Journal createJournalEntry(JournalData journalData) {
        
        if (journalData.getTransactionId()!=null && findJournalEntry(journalData.getTransactionId()).isPresent()) {
            throw APIException.conflict("Journal entry {0} already exists.", journalData.getTransactionId());
        }
        if (journalData.getDebit().isEmpty()) {
            throw APIException.badRequest("Debtors must be given.");
        }
        if (journalData.getCredit().isEmpty()) {
            throw APIException.badRequest("Creditors must be given.");
        }

        final Double debtorAmountSum = journalData.getDebit()
                .stream()
                .peek(debtor -> {
                    final Optional<Account> accountOptional = accountService.findAccount(debtor.getAccountNumber());
                    if (!accountOptional.isPresent()) {
                        throw APIException.badRequest("Unknown debtor account {0}.", debtor.getAccountNumber());
                    }
                    if (!accountOptional.get().getEnabled()) {
                        throw APIException.badRequest("Debtor account {0} must be enabled for Transaction", debtor.getAccountNumber());
                    }
                })
                .map(debtor -> Double.valueOf(debtor.getAmount()))
                .reduce(0.0D, (x, y) -> x + y);

        final Double creditorAmountSum = journalData.getCredit()
                .stream()
                .peek(creditor -> {
                    final Optional<Account> accountOptional = accountService.findAccount(creditor.getAccountNumber());
                    if (!accountOptional.isPresent()) {
                        throw APIException.badRequest("Unknown creditor account{0}.", creditor.getAccountNumber());
                    }
                    if (!accountOptional.get().getEnabled()) {
                        throw APIException.badRequest("Creditor account {0} must be enabled for Transaction.", creditor.getAccountNumber());
                    }
                })
                .map(creditor -> Double.valueOf(creditor.getAmount()))
                .reduce(0.0D, (x, y) -> x + y);

        if (!debtorAmountSum.equals(creditorAmountSum)) {
            throw APIException.badRequest("Sum of debtor and sum of creditor amounts must be equals.");
        }
        
        Journal journal = convertToEntity(journalData);
        journal.setTransactionId(generateTransactionId(2L));
//           journal.setTransactionId(sequenceService.nextNumber(SequenceType.JournalNumber));       
        Journal savedJournal = journalRepository.save(journal);
        return savedJournal;
    }

    public Optional<Journal> findJournalEntry(final String transactionIdentifier) {
        return journalRepository.findByTransactionId(transactionIdentifier);
    }

    public JournalData findJournalDataEntry(final String transactionIdentifier) {
        Journal journal = findJournalEntry(transactionIdentifier)
                .orElseThrow(() -> APIException.notFound("Journal number {0} not found.", transactionIdentifier));

        return JournalData.map(journal);
    }

    public Page<JournalData> fetchJournalEntries(String referenceNumber, String transactionId, String transactionType, DateRange range, Pageable pageable) {
        Specification<Journal> spec = JournalSpecification.createSpecification(referenceNumber, transactionId, transactionType, range);
        Page<Journal> journals = journalRepository.findAll(spec, pageable);
        return journals.map(journal -> JournalData.map(journal));
    }

    private Journal convertToEntity(JournalData journalData) {
        Journal journal = new Journal();
        
        journal.setActivity(journalData.getActivity());
        journal.setDescriptions(journalData.getDescriptions());
        journal.setDocumentDate(journalData.getDocumentDate());
        journal.setManualEntry(journalData.isManualEntry());
        journal.setReferenceNumber(journalData.getReferenceNumber());
        journal.setState(journalData.getState());
        journal.setTransactionDate(journalData.getTransactionDate());
        journal.setTransactionId(journalData.getTransactionId());
        journal.setTransactionType(journalData.getTransactionType());

        journalData.getDebit()
                .stream()
                .map(jd -> new JournalEntry(getAccount(jd.getAccountNumber()), 0.00, Double.valueOf(jd.getAmount()), journalData.getTransactionDate(), jd.getDescription()))
                .forEach(je -> journal.addJournalEntry(je));

        journalData.getCredit()
                .stream()
                .map(jd -> new JournalEntry(getAccount(jd.getAccountNumber()), Double.valueOf(jd.getAmount()), 0.00, journalData.getTransactionDate(),jd.getDescription()))
                .forEach(je -> journal.addJournalEntry(je));

        return journal;
    }

    public String revertJournalEntry(String transactionId, String reversalComment) {
        
        final String reversalTransactionId = generateTransactionId(2L);// sequenceService.nextNumber(SequenceType.JournalNumber);//generateTransactionId(2L);
        final boolean manualEntry = true;
        final boolean useDefaultComment = StringUtils.isBlank(reversalComment);

        Journal journalEntry = journalRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> APIException.notFound("Transactions {0} Not Found", transactionId));
        // check if the period is closed
        
//        if(journalEntry.isReversed()){
//            throw APIException.badRequest("Transaction Id {0} is already Reversed", transactionId);
//        }
        if (useDefaultComment) {
            reversalComment = "Reversal entry for Journal Entry with Entry Id  :" + journalEntry.getId() + " and transaction Id " + journalEntry.getTransactionId();
        }
        Journal reversalJournal = new Journal();
        reversalJournal.setDescriptions(reversalComment);
        reversalJournal.setActivity(journalEntry.getActivity());
        reversalJournal.setDocumentDate(journalEntry.getDocumentDate());
        reversalJournal.setManualEntry(manualEntry);
        reversalJournal.setReferenceNumber(journalEntry.getReferenceNumber());
        reversalJournal.setTransactionDate(journalEntry.getTransactionDate());
        reversalJournal.setTransactionId(reversalTransactionId);

        journalEntry.getJournalEntries()
                .forEach((je) -> {
                    if (je.isDebit()) {
                        reversalJournal.addJournalEntry(new JournalEntry(je.getAccount(), je.getDebit(), 0.0D, je.getEntryDate(),je.getDescription()));
                    } else {
                        reversalJournal.addJournalEntry(new JournalEntry(je.getAccount(), 0.0D, je.getCredit(), je.getEntryDate(),je.getDescription()));
                    }
                });
         Journal savedJournal = journalRepository.save(reversalJournal);
         
        journalEntry.setReversed(true);
        journalEntry.setState(JournalState.REVERSED);
        journalEntry.setReversalJournal(savedJournal);
        
        journalRepository.save(journalEntry);
        return reversalTransactionId;
    }
 
    public static String generateTransactionId(final Long companyId) {
        //journal format : ACC-JV-2019-00001
//        Long id = SecurityUtils.getCurrentLoggedUserId().get();
        final Long time = System.currentTimeMillis();
        final String uniqueVal = String.valueOf(time) + 120L + companyId;
        final String transactionId = Long.toHexString(Long.parseLong(uniqueVal));
        return transactionId;
    }

    private Account getAccount(String accountNo) {
        return  accountService.findOneWithNotFoundDetection(accountNo);
    }
    public void doJournalBalances(){
        balanceUpdateService.updateRunningBalance();
    }
    
    private void validateAccountForTransaction(final Account account) {
        /***
         * validate that the account allows manual adjustments and is not
         * disabled
         **/
        if (!account.getEnabled()) {
            throw  APIException.badRequest("Target account is not enabled");
        } 
    }
}
