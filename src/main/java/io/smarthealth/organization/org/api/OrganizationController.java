package io.smarthealth.organization.org.api;

import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.organization.org.data.OrganizationData;
import io.smarthealth.organization.org.domain.Organization;
import io.smarthealth.organization.org.service.OrganizationService;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@RequestMapping("/api")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping("/organization")
    public @ResponseBody
    ResponseEntity<?> createOrganization(@RequestBody @Valid final OrganizationData orgData) {
        OrganizationData result = service.createOrganization(orgData);
        Pager<OrganizationData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Organization created successfully");
        pagers.setContent(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping("/organization/{id}")
    public OrganizationData getOrganization(@PathVariable(value = "id") String id) {
        Organization org = service.getOrganization(id);
        return OrganizationData.map(org);
    }

    @GetMapping("/organization")
    public OrganizationData getActiveOrganization() {
        Organization org = service.getActiveOrganization();

        return OrganizationData.map(org);
    }
    
    @PutMapping("/organization/{id}")
    public OrganizationData OrganizationData(@PathVariable(value = "id") String id, OrganizationData data) {
        return service.updateOrganization(id, data);
    }
     
}
