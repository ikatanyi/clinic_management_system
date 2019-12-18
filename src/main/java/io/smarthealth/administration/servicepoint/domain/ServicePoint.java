package io.smarthealth.administration.servicepoint.domain;

import io.smarthealth.accounting.account.data.SimpleAccountData;
import io.smarthealth.accounting.account.domain.Account;
import io.smarthealth.administration.servicepoint.data.ServicePointData;
import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "service_points")
public class ServicePoint extends Identifiable {

    private String name;
    private String description;
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_service_point_income_account_id"))
    private Account incomeAccount;
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_service_point_expense_account_id"))
    private Account expenseAccount;
    private Boolean active;

    public ServicePointData toData() {
        ServicePointData data = new ServicePointData();
        data.setId(this.getId());
        data.setActive(this.getActive());
        data.setName(this.getName());
        data.setDescription(this.getDescription());
        if (this.getIncomeAccount() != null) {
            data.setIncomeAccount(SimpleAccountData.map(this.getIncomeAccount()));
        }
        if (this.getExpenseAccount() != null) {
            data.setExpenseAccount(SimpleAccountData.map(this.getExpenseAccount()));
        }
        return data;
    }
}
