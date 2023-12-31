package io.smarthealth.administration.finances.domain;

import io.smarthealth.administration.finances.data.PaymentMethodData;
import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table; 
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author Kelsas
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity 
@Table(name = "ref_payment_method")
public class PaymentMethod extends Identifiable {

    @Column(name = "value")
    private String name;
    private String description;
    private Boolean isCashPayment; 
    private Boolean active;

    public PaymentMethodData toData() {
        PaymentMethodData data = new PaymentMethodData();
        data.setId(this.getId());
        data.setActive(this.getActive());
        data.setName(this.getName());
        data.setDescription(this.getDescription());
        data.setIsCashPayment(this.getIsCashPayment()); 
        return data;
    }
}
