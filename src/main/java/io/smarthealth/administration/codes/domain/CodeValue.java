package io.smarthealth.administration.codes.domain;

import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.*;
import lombok.Data;
 
@Data
@Entity
@Table(name = "m_code_value",
        uniqueConstraints = {
        @UniqueConstraint(
                columnNames = { "code_id", "code_value" },
                name = "code_value_duplicate")
})
public class CodeValue extends Identifiable {

    @Column(name = "code_value", length = 100)
    private String label;

    @Column(name = "order_position")
    private int position;

    @Column(name = "code_description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "code_id", nullable = false)
    private Code code;

    @Column(name = "is_active")
    private boolean isActive;
    
    @Column(name = "is_mandatory")
    private boolean mandatory;
 
}