package io.smarthealth.clinical.admission.data;

import io.smarthealth.clinical.admission.domain.EmergencyContact;
import io.swagger.annotations.ApiModelProperty;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class EmergencyContactData {
    @ApiModelProperty(hidden=true)
    private Long id;
    @ApiModelProperty(hidden=true)
    private Long admissionId;
    @ApiModelProperty(example="Parent,Spouse, Sibling, Friend")
    @Enumerated(EnumType.STRING)
    public EmergencyContact.Relation relation;
    private String name;
    private String contactNumber;
    
    public EmergencyContact map() {
        EmergencyContact data = new EmergencyContact();
        data.setId(this.getId());
        data.setName(this.getName());
        data.setRelation(this.getRelation());
        data.setContactNumber(this.getContactNumber());
        return data;
    }
}
