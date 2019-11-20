package io.smarthealth.accounting.account.domain;

import io.smarthealth.accounting.account.data.JournalEntryData;
import io.smarthealth.infrastructure.domain.Auditable;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "account_journal_entry")
public class JournalEntry extends Auditable {

    @ManyToOne
    @JoinColumn(name = "journal_id", foreignKey = @ForeignKey(name = "fk_journal_journal_id"))
    private Journal journal;

    @ManyToOne
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_journal_account_id"))
    private Account account;

    private Double credit = 0D;
    private Double debit = 0D;
    private String description;//optional comments about the entry
    private LocalDate entryDate;
    private boolean isBalanceCalculated = false;
    private BigDecimal runningBalance;

    public JournalEntry() {
        super();
    }

    public JournalEntry(Account account, Double credit, Double debit, LocalDate entryDate, String description) {
        this.account = account;
        this.credit = credit;
        this.debit = debit;
        this.entryDate = entryDate;
        this.description = description;
    }

    public boolean isDebit() {
        return debit != 0;
    }

    public JournalEntry getRevertedJournalEntry() {
        return new JournalEntry(this.getAccount(), this.getDebit(), this.getCredit(), this.getEntryDate(), this.getDescription());
    }

    public BigDecimal getAmount() {
        if (isDebit()) {
            return BigDecimal.valueOf(debit);
        }
        return BigDecimal.valueOf(credit);
    }

    public JournalEntryData toData() {
        return new JournalEntryData(
                this.getId(),
                this.getEntryDate(),
                this.getJournal().getTransactionId(),
                this.getAccount().getAccountNumber(),
                this.getAccount().getAccountName(),
                this.getCredit(),
                this.getDebit(),
                this.getDescription(),
                this.getRunningBalance(),
                this.isBalanceCalculated());
    }
}
