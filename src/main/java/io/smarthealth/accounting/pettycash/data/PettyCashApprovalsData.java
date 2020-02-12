/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.accounting.pettycash.data;

import io.smarthealth.accounting.pettycash.data.enums.PettyCashStatus;
import io.smarthealth.accounting.pettycash.domain.PettyCashApprovals;
import io.swagger.annotations.ApiModelProperty;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Simon.waweru
 */
@Data
public class PettyCashApprovalsData {
    
    @ApiModelProperty(hidden = true)
    private String staffNumber;
    
    @ApiModelProperty(hidden = true)
    private String staffName;
    
    @Enumerated(EnumType.STRING)
    private PettyCashStatus approvalStatus;
    
    private String approvalComments;
    
    public static PettyCashApprovalsData map(PettyCashApprovals entity) {
        PettyCashApprovalsData data = new PettyCashApprovalsData();
        data.setApprovalComments(entity.getApprovalComments());
        data.setApprovalStatus(entity.getApprovalStatus());
        data.setStaffName(entity.getEmployee().getFullName());
        data.setStaffNumber(entity.getEmployee().getStaffNumber());
        return data;
    }
}