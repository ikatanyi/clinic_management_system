package io.smarthealth.accounting.accounts.data.financial.statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class FinancialCondition {

    @NotEmpty
    private String date;
    @JsonIgnore
    private LocalDate asAt;
    @NotEmpty
    private List<FinancialConditionSection> financialConditionSections = new ArrayList<>();
    @NotNull
    private BigDecimal totalAssets;
    @NotNull
    private BigDecimal totalEquitiesAndLiabilities;

    public FinancialCondition() {
        super();
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public List<FinancialConditionSection> getFinancialConditionSections() {
        return this.financialConditionSections;
    }

    public BigDecimal getTotalAssets() {
        return this.totalAssets;
    }

    public void setTotalAssets(final BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalEquitiesAndLiabilities() {
        return this.totalEquitiesAndLiabilities;
    }

    public void setTotalEquitiesAndLiabilities(final BigDecimal totalEquitiesAndLiabilities) {
        this.totalEquitiesAndLiabilities = totalEquitiesAndLiabilities;
    }

    public void add(final FinancialConditionSection financialConditionSection) {
        this.financialConditionSections.add(financialConditionSection);
    }

    public LocalDate getAsAt() {
        return asAt;
    }

    public void setAsAt(LocalDate asAt) {
        this.asAt = asAt;
    }

}
