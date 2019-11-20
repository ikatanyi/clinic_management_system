package io.smarthealth.administration.app.domain;

import io.smarthealth.administration.app.data.PaymentMethodData;
import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "app_payment_method")
public class PaymentMethod extends Identifiable {

    @Column(name = "value")
    private String name;
    private String description;
    private Boolean isCashPayment;
    @Column(name = "order_position")
    private Long position;
    private Boolean active;

    public PaymentMethodData toData() {
        PaymentMethodData data = new PaymentMethodData();
        data.setId(this.getId());
        data.setActive(this.getActive());
        data.setName(this.getName());
        data.setDescription(this.getDescription());
        data.setIsCashPayment(this.getIsCashPayment());
        data.setPosition(this.getPosition());
        return data;
    }
}
