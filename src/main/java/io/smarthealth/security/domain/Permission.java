package io.smarthealth.security.domain;
  
import io.smarthealth.infrastructure.domain.Identifiable;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;

/**
 * System Permission
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "auth_permission")
public class Permission extends Identifiable {
 
    private String permissionGroup;

    private String name;

    public boolean hasCode(final String checkCode) {
        return this.name.equalsIgnoreCase(checkCode);
    }

}