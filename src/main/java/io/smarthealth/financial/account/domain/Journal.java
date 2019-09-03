package io.smarthealth.financial.account.domain;

import io.smarthealth.financial.payment.domain.PaymentDetail;
import io.smarthealth.infrastructure.domain.Auditable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "account_journal")
public class Journal extends Auditable {

    private LocalDate transactionDate;
    private String transactionId;
    private String transactionType;
    private LocalDate documentDate;
    private String activity; // financial activity accured - Department
    private String descriptions; //A description associated with this entry
    private String referenceNumber; //An additional field that is used to store additional information about the entry (Ex: chequeNo, receipt no)
    @ManyToOne(optional = true)
    @JoinColumn(name = "payment_details_id", nullable = true)
    private PaymentDetail paymentDetail; //payment details about the transactions 
    private boolean manualEntry; //Flag determines if an entry is generated by system or entered manually
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_id")
    private Journal reversalJournal;
    private boolean reversed;
    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL)
    private List<JournalEntry> journalEntries = new ArrayList<>();
//        private Set<JournalEntry> journalEntries=new HashSet<>();

    public void addJournalEntry(JournalEntry journalEntry) {
        journalEntries.add(journalEntry);
        journalEntry.setJournal(this);
    }
}

//
