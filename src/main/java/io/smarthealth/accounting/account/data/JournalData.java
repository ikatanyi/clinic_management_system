package io.smarthealth.accounting.account.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.smarthealth.accounting.account.domain.Journal;
import io.smarthealth.accounting.account.domain.enumeration.JournalState;
import io.smarthealth.infrastructure.lang.Constants;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 *
 * @author Kelsas
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JournalData {

    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate transactionDate = LocalDate.now();
    private String transactionId;
    private String transactionType;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate documentDate;
    private String activity; // financial activity accured - Department
    @Length(max = 2048)
    private String descriptions; //A description associated with this entry
    private String referenceNumber; //An additional field that is used to store additional information about the entry (Ex: chequeNo, receipt no)
    private Long paymentDetail = 0L; //payment details about the transactions
    private JournalState state = JournalState.DRAFT; // state of transactions
    private boolean manualEntry; //Flag determines if an entry is generated by system or entered manually
    private Set<Debit> debit;
    private Set<Credit> credit;
    private List<JournalEntryData> journalEntries;
    private Double journalTotals;

    public Double getJournalTotals() {
        Double dbts = debit
                .stream()
                .map(d -> Double.valueOf(d.getAmount()))
                .reduce(0D, Double::sum);
        Double crts = credit
                .stream()
                .map(d -> Double.valueOf(d.getAmount()))
                .reduce(0D, Double::sum);
        journalTotals = dbts > 0 ? dbts : crts;
        return journalTotals;
    }

    public static JournalData map(Journal journal) {
        JournalData journalData = new JournalData();
        journalData.setActivity(journal.getActivity());
        journalData.setDescriptions(journal.getDescriptions());
        journalData.setDocumentDate(journal.getDocumentDate());
        journalData.setManualEntry(journal.isManualEntry());
        journalData.setReferenceNumber(journal.getReferenceNumber());
        journalData.setState(journal.getState());
        journalData.setTransactionDate(journal.getTransactionDate());
        journalData.setTransactionId(journal.getTransactionId());
        journalData.setTransactionType(journal.getTransactionType());

        Set<Debit> debits = new HashSet<>();
        Set<Credit> credit = new HashSet<>();
        journal.getJournalEntries()
                .stream()
                .forEach(je -> {
                    if (je.isDebit()) {
                        debits.add(new Debit(je.getAccount().getAccountNumber(), String.valueOf(je.getDebit()), je.getDescription()));
                    } else {
                        credit.add(new Credit(je.getAccount().getAccountNumber(), String.valueOf(je.getCredit()), je.getDescription()));
                    }
                });
        
        List<JournalEntryData> lists = journal.getJournalEntries()
                .stream().map(je -> JournalEntryData.map(je)).collect(Collectors.toList());
        
        journalData.setJournalEntries(lists);
        
        journalData.setCredit(credit);
        journalData.setDebit(debits);

        return journalData;
    }
}
