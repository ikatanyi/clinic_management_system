/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.admission.api;

import io.smarthealth.clinical.admission.data.AdmissionData;
import io.smarthealth.clinical.admission.data.BedData;
import io.smarthealth.clinical.admission.data.CareTeamData;
import io.smarthealth.clinical.admission.domain.Admission;
import io.smarthealth.clinical.admission.domain.Bed;
import io.smarthealth.clinical.admission.domain.CareTeamRole;
import io.smarthealth.clinical.admission.service.CareTeamService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Simon.waweru
 */
@Api
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CareTeamController {

    private final CareTeamService careTeamService;
    private final AuditTrailService auditTrailService; 

    @PostMapping("/care-team")
//    @PreAuthorize("hasAuthority('create_care_team')")
    public ResponseEntity<?> createCareTeam(@Valid @RequestBody List<CareTeamData> data) {
        List<CareTeamData> ct = careTeamService.createCareTeam(data).stream()
                .map(e -> {
                     auditTrailService.saveAuditTrail("Admission", "Added Care team "+e.getMedic().getFullName());
                    return CareTeamData.map(e);
                        })
                .collect(Collectors.toList());

        Pager< List<CareTeamData>> pagers = new Pager();
        pagers.setCode("200");
        pagers.setMessage("Care Team successfully submitted");
        pagers.setContent(ct);
       
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping("/care-team/{admissionNumber}")
//    @PreAuthorize("hasAuthority('view_care_team')")
    public ResponseEntity<?> viewCareTeam(
            @PathVariable("admissionNumber") final String admissionNumber
    ) {

        List<CareTeamData> ct = careTeamService.fetchCareTeamByAdmissionNumber(admissionNumber).stream()
                .map(e ->{
                    auditTrailService.saveAuditTrail("Admission", "Added Care team "+e.getMedic().getFullName());
                   return  CareTeamData.map(e);
                        })
                .collect(Collectors.toList());

        Pager< List<CareTeamData>> pagers = new Pager();
        pagers.setCode("200");
        pagers.setMessage("Care team successfully submitted");
        pagers.setContent(ct);

        return ResponseEntity.status(HttpStatus.OK).body(pagers);
    }
    
    @GetMapping("/care-team")
//    @PreAuthorize("hasAuthority('view_bed')")
    public ResponseEntity<?> getCareTeams(
            @RequestParam(value = "active", required = false, defaultValue = "false") final Boolean active,
            @RequestParam(value = "patientNumber", required = false) final String patientNo,
            @RequestParam(value = "admissionNumber", required = false) final String admissionNo,
            @RequestParam(value = "careRole", required = false) final CareTeamRole careRole,          
            @RequestParam(value = "voided", required = false) final Boolean voided,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {
        Pageable pageable = PaginationUtil.createPage(page, size);
        List<CareTeamData> list =  careTeamService.getCareTeams(patientNo, admissionNo, careRole, active,  voided, pageable)
                                .getContent()
                                .stream()                                
                                .map(e -> { 
                                    auditTrailService.saveAuditTrail("Admission", "Viewed Care team "+e.getMedic().getFullName() +" for admitted patient "+e.getPatient().getFullName());
                                        return CareTeamData.map(e);
                                })
                                .collect(Collectors.toList());
        
        
        Pager<List<CareTeamData>> pagers=new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list);
        PageDetails details=new PageDetails();
        details.setPage(list.size()+1);
        details.setPerPage(list.size());
//        details.setTotalElements(list.g);
//        details.setTotalPage(list.getTotalPages());
        details.setReportName("Careteams");
        pagers.setPageDetails(details);
         
        return ResponseEntity.ok(pagers);
    }
    
    @PutMapping("/care-team/{id}")
//    @PreAuthorize("hasAuthority('create_admission')")
    public ResponseEntity<?> updateCareTeam(@PathVariable("id") Long id, @Valid @RequestBody CareTeamData careTeamData) {

        CareTeamData a = CareTeamData.map(careTeamService.updateCareTeam(id, careTeamData));
        auditTrailService.saveAuditTrail("Admission", "Edited Care team identified by id "+id);
        Pager<CareTeamData> pagers = new Pager();
        pagers.setCode("200");
        pagers.setMessage("Care team Updated successfully");
        pagers.setContent(a);

        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }
    
    @PostMapping("/care-team/add")
//    @PreAuthorize("hasAuthority('create_admission')")
    public ResponseEntity<?> addCareteam(@Valid @RequestBody List<CareTeamData> careTeamData) {

        List<CareTeamData> ct = careTeamService.addCareTeam(careTeamData).stream()
                .map(e -> {   
                    auditTrailService.saveAuditTrail("Admission", "Added Care team "+e.getMedic().getFullName());
                    return CareTeamData.map(e);
                        })
                .collect(Collectors.toList());
        
        Pager< List<CareTeamData>> pagers = new Pager();
        pagers.setCode("200");
        pagers.setMessage("Care Team successfully added");
        pagers.setContent(ct);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }
    
    @DeleteMapping("/care-team/{id}")
//    @PreAuthorize("hasAuthority('create_admission')")
    public ResponseEntity<?> removeCareteam(@PathVariable("id") Long id, @Valid @RequestParam(value="reason", required=false) String reason) {
        
        auditTrailService.saveAuditTrail("Admission", "Deleted Care team identified by id "+id);
        careTeamService.removeCareTeam(id, reason);
        Pager<AdmissionData> pagers = new Pager();
        pagers.setCode("200");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pagers);
    }

}
