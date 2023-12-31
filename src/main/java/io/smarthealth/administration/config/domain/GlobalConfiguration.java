package io.smarthealth.administration.config.domain;

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
@Table(name = "app_configuration")
public class GlobalConfiguration extends Identifiable {

    @Column(name = "config_name")
    private String name;
    @Column(name = "config_value")
    private String value;
    private String description;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    private String fieldType;
    
     @Column(name = "config_group")
    private String group;
}
