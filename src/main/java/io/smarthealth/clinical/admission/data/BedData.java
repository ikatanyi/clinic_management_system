package io.smarthealth.clinical.admission.data;

import io.smarthealth.clinical.admission.domain.Bed;
import io.smarthealth.clinical.admission.domain.Bed.Status;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class BedData {
    @ApiModelProperty(hidden=true)
    private Long id;
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    public Status status;
    private Long roomId;
    @ApiModelProperty(hidden=true)
    private String room;
    private Boolean active;
    private Integer col;
    private Integer row;
    private List<BedChargeData> bedCharges = new ArrayList<>();
    
    public Bed map() {
        Bed data = new Bed();
        data.setId(this.getId());
        data.setIsActive(this.getActive());
        data.setDescription(this.description);
        data.setStatus(this.getStatus()); 
        data.setCol(this.col);
        data.setRow(this.row);
        return data;
    }
}
