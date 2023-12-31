package io.smarthealth.stock.stores.api;

import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.security.service.AuditTrailService;
import io.smarthealth.stock.stores.data.StoreData;
import io.smarthealth.stock.stores.domain.Store;
import io.smarthealth.stock.stores.service.StoreService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@Slf4j
@RequestMapping("/api")
public class StoreRestController {
    private final StoreService service;
    private final AuditTrailService auditTrailService;

    public StoreRestController(StoreService service, AuditTrailService auditTrailService) {
        this.service = service;
        this.auditTrailService = auditTrailService;
    }
    @PostMapping("/stores")
    @PreAuthorize("hasAuthority('create_stores')")
    public ResponseEntity<?> createStore(@Valid @RequestBody StoreData data) {
        
        Store result = service.createStore(data);
        
        Pager<StoreData> pagers=new Pager();
        pagers.setCode("0");
        pagers.setMessage("Store created success");
        pagers.setContent(StoreData.map(result)); 
        auditTrailService.saveAuditTrail("Stores", "Created store "+result.getStoreName());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagers);
    }
    @GetMapping("/stores/{id}")
    @PreAuthorize("hasAuthority('view_stores')")
    public ResponseEntity<?> getStore(@PathVariable(value = "id") Long code) {
        Store store = service.getStore(code)
                .orElseThrow(() -> APIException.notFound("Store with id  {0} not found.", code));
        auditTrailService.saveAuditTrail("Stores", "Viewed store "+store.getStoreName());
        return ResponseEntity.ok(StoreData.map(store));
        
    }
    @PutMapping("/stores/{id}")
    @PreAuthorize("hasAuthority('view_stores')")
    public ResponseEntity<?> updateStore(@PathVariable(value = "id") Long id, @Valid @RequestBody StoreData data) {
        Store store = service.updateStore(id, data);
        auditTrailService.saveAuditTrail("Stores", "Edited store "+store.getStoreName());
        return ResponseEntity.ok(StoreData.map(store));
        
    }
    
    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('view_stores')")
    public ResponseEntity<?> getAllStorees( 
             @RequestParam(value = "isPatientStore", required = false) Boolean patientStore,
             @RequestParam(value = "storeType", required = false) Store.Type storeType,
             @RequestParam(value = "servicePointType", required = false) ServicePointType servicePointType,
                     @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);

        Page<StoreData> list = service.fetchAllStores(patientStore,storeType, servicePointType,pageable).map(store -> StoreData.map(store));
        Pager<List<StoreData>> pagers=new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details=new PageDetails();
        details.setPage(list.getNumber()+1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Stores");
        pagers.setPageDetails(details);
        auditTrailService.saveAuditTrail("Stores", "Viewed all stores");
        return ResponseEntity.ok(pagers);
    }
    
     @GetMapping("/stores/$metadata")
     @PreAuthorize("hasAuthority('view_stores')")
    public ResponseEntity<?> getStoresMetadata(){
        
        return ResponseEntity.ok(service.getStoreMetadata());
    }
}
