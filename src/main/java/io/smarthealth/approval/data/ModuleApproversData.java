/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.approval.data;

import io.smarthealth.approval.data.enums.ApprovalModule;
import io.smarthealth.approval.domain.ModuleApprovers;
import io.swagger.annotations.ApiModelProperty;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Simon.waweru
 */
@Data
public class ModuleApproversData {
    
    private String staffNumber;
    @ApiModelProperty(required = false, hidden = true)
    private String staffName;
    private int approvalLevel;
    @Enumerated(EnumType.STRING)
    private ApprovalModule moduleName;
    private Long id;
    
    public static ModuleApproversData map(ModuleApprovers approver) {
        ModuleApproversData data = new ModuleApproversData();
        data.setModuleName(approver.getModuleName());
        data.setApprovalLevel(approver.getApprovalLevel());
        data.setStaffName(approver.getEmployee().getFullName());
        data.setStaffNumber(approver.getEmployee().getStaffNumber());
        data.setId(approver.getId());
        return data;
    }

//    public static ModuleApprovers map(ModuleApproversData data){
//        ModuleApprovers approver = new ModuleApprovers();
//        approver.set
//    }
}
