package io.smarthealth.organization.company.api;

import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.organization.company.data.CompanyData;
import io.smarthealth.organization.company.data.LogoResponse;
import io.smarthealth.organization.company.domain.Company;
import io.smarthealth.organization.company.domain.CompanyLogo;
import io.smarthealth.organization.company.service.CompanyService;
import io.smarthealth.security.service.AuditTrailService;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@RequestMapping("/api")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping("/company")
    @PreAuthorize("hasAuthority('create_company')")
    public @ResponseBody
    ResponseEntity<?> createCompany(@RequestBody @Valid final CompanyData orgData) {
        Company result = service.createOrganization(orgData);
        Pager<CompanyData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Organization created successfully");
        pagers.setContent(result.toData());

        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }

    @GetMapping("/company/{id}")
    @PreAuthorize("hasAuthority('view_company')")
    public @ResponseBody
    ResponseEntity<?> getCompany(@PathVariable(value = "id") String id) {
        Company result = service.getOrganizationOrThrow(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.toData());
    }

    @GetMapping("/company/current")
    @PreAuthorize("hasAuthority('view_company')")
    public @ResponseBody
    ResponseEntity<?> getCurrentCompany() {
        Company result = service.getCurrentOrganization();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.toData());
    }

    @PutMapping("/company/{id}")
    @PreAuthorize("hasAuthority('edit_company')")
    public @ResponseBody
    ResponseEntity<?> updateCompany(@PathVariable(value = "id") String id, CompanyData data) {
        Company result = service.updateOrganization(id, data);
        Pager<CompanyData> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Organization profile updated successfully");
        pagers.setContent(result.toData());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pagers);
    }

    //Your logo has been saved.
    @PostMapping("/company/{id}/logo")
    @PreAuthorize("hasAuthority('create_company')")
    public LogoResponse uploadLogo(@PathVariable("id") final String id, @RequestParam("file") MultipartFile file) {
        CompanyLogo logo = service.storeLogo(id, file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/company/logo/")
                .path(String.valueOf(logo.getId()))
                .toUriString();
        return new LogoResponse(logo.getFileName(), fileDownloadUri, file.getContentType(), file.getSize());
    }

    @GetMapping("/company/logo/{logoId}")
    @PreAuthorize("hasAuthority('view_company')")
    public ResponseEntity<Resource> downloadLogo(@PathVariable Long logoId) {
        // Load file from database
        CompanyLogo dbFile = service.getLogo(logoId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dbFile.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dbFile.getFileName() + "\"")
                .body(new ByteArrayResource(dbFile.getData()));
    }

    @DeleteMapping("/company/logo/{logoId}")
    @PreAuthorize("hasAuthority('delete_company')")
    public ResponseEntity<?> deleteLogo(@PathVariable Long logoId) {
        service.deleteLogo(logoId);
        return ResponseEntity.noContent().build();
    }
}
