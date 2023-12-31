package io.smarthealth.stock.stores.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.smarthealth.accounting.accounts.domain.Account;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.infrastructure.domain.Identifiable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 *
 * @author Kelsas
 */
@Entity
@Getter
@Setter
@Table(name = "st_stores")
public class Store extends Identifiable {

    public enum Type {
        MainStore,
        SubStore,
    }
    private Type storeType;
    private String storeName;
    private boolean patientStore;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_store_service_point_id"))
    private ServicePoint servicePoint;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_store_inventory_account_id"))
    private Account inventoryAccount;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_store_inventory_expense_account_id"))
    private Account expenseAccount;

    private boolean active;

    @Override
    public String toString() {
        return "Store [id=" + getId() + ", name=" + storeName + ", type=" + storeType + "]";
    }
}
