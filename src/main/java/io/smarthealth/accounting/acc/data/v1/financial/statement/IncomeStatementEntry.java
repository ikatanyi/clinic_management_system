package io.smarthealth.accounting.acc.data.v1.financial.statement;

import java.math.BigDecimal;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class IncomeStatementEntry {

    @NotEmpty
    private String description;
    @NotNull
    private BigDecimal value;

    public IncomeStatementEntry() {
        super();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }
}
