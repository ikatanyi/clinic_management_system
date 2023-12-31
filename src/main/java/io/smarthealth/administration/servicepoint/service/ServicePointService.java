package io.smarthealth.administration.servicepoint.service;

import io.smarthealth.accounting.accounts.domain.Account;
import io.smarthealth.accounting.accounts.service.AccountService;
import io.smarthealth.administration.servicepoint.data.ServicePointData;
import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.infrastructure.exception.APIException;
import java.util.Objects;

import io.smarthealth.stock.stores.domain.Store;
import io.smarthealth.stock.stores.domain.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.smarthealth.administration.servicepoint.domain.ServicePointRepository;
import io.smarthealth.administration.servicepoint.domain.specification.ServicePointSpecification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author Kelsas
 */
@Service
public class ServicePointService {

    private final ServicePointRepository repository;
    private final AccountService accountService;
    private final StoreRepository storeRepository;

    public ServicePointService(ServicePointRepository repository, AccountService accountService, StoreRepository storeRepository) {
        this.repository = repository;
        this.accountService = accountService;
        this.storeRepository = storeRepository;
    }

    public ServicePointData createPoint(ServicePointData data) {
        ServicePoint point = new ServicePoint();
        if (data.getServicePointType().equals(ServicePointType.Consultation) || data.getServicePointType().equals(ServicePointType.Triage)) {
            //find if servicepoint type exists
            Optional<ServicePoint> sp = repository.findServicePointByServicePointType(data.getServicePointType());
            if (sp.isPresent()) {
                throw APIException.conflict("Service point type {0} has already been registered", data.getServicePointType().name());
            }
        }
        if (data.getServicePointType().equals(ServicePointType.Consultation)) {

        }
        point.setActive(data.getActive());
        point.setDescription(data.getDescription());
        point.setName(data.getName());
        point.setPointType(data.getPointType());
        point.setServicePointType(data.getServicePointType());
        if (data.getExpenseAccount() != null && data.getExpenseAccount().getAccountNumber() != null || !"".equals(data.getExpenseAccount().getAccountNumber())) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getExpenseAccount().getAccountNumber());
            point.setExpenseAccount(acc);
        }

        if (data.getIncomeAccount() != null && data.getIncomeAccount().getAccountNumber() != null) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getIncomeAccount().getAccountNumber());
            point.setIncomeAccount(acc);
        }

        if (data.getInventoryAssetAccount() != null && data.getInventoryAssetAccount().getAccountNumber() != null) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getInventoryAssetAccount().getAccountNumber());
            point.setInventoryAssetAccount(acc);
        }
        if(data.getStoreId()!=null){
            Store store = storeRepository.findById(data.getStoreId())
                    .orElseThrow(() -> APIException.notFound("Store with ID {} not found", data.getStoreId()));
            point.setStore(store);
        }

        ServicePoint savedPoint = repository.save(point);
        return savedPoint.toData();
    }

    public ServicePoint getServicePoint(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> APIException.notFound("Service point with id {0} not found", id));
    }

    public List<ServicePoint> getServiceLocationsByType(final ServicePointType servicePointType) {
        return repository.findByServicePointType(servicePointType);//.orElseThrow(() -> APIException.notFound("Service point identified by  {0} not found", servicePointType.name()));
    }

    public List<ServicePoint> getServiceLocations(final ServicePointType servicePointType) {
        return repository
                .findByServicePointType(servicePointType);
    }

    public ServicePoint getServicePointByType(final ServicePointType servicePointType) {
        return repository.findByServicePointType(servicePointType).get(0);//.orElseThrow(() -> APIException.notFound("Service point identified by  {0} not found", servicePointType.name()));
    }

//    public Optional<ServicePoint> getServicePoint(final ServicePointType servicePointType) {
//        return repository.findByServicePointType(servicePointType).get(0);
//    }
    public Page<ServicePoint> listServicePoints(ServicePointType servicePointType, String pointType, Pageable page) {
        Specification<ServicePoint> spec = ServicePointSpecification.createSpecification(servicePointType, pointType);
        return repository.findAll(spec, page);
    }

    public ServicePointData updateServicePoint(Long id, ServicePointData data) {
        ServicePoint point = getServicePoint(id);
        if (!Objects.equals(point.getActive(), data.getActive())) {
            point.setActive(data.getActive());
        }
        if (!point.getName().equals(data.getName())) {
            point.setName(data.getName());
        }
        if (!point.getDescription().equals(data.getDescription())) {
            point.setDescription(data.getDescription());
        }
        if (data.getExpenseAccount() != null && data.getExpenseAccount().getAccountNumber() != null || !"".equals(data.getExpenseAccount().getAccountNumber())) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getExpenseAccount().getAccountNumber());
            point.setExpenseAccount(acc);
        }

        if (data.getIncomeAccount() != null && data.getIncomeAccount().getAccountNumber() != null) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getIncomeAccount().getAccountNumber());
            point.setIncomeAccount(acc);
        }

        if (data.getInventoryAssetAccount() != null && data.getInventoryAssetAccount().getAccountNumber() != null) {
            Account acc = accountService.findByAccountNumberOrThrow(data.getInventoryAssetAccount().getAccountNumber());
            point.setInventoryAssetAccount(acc);
        }
        if(data.getStoreId()!=null){
            Store store = storeRepository.findById(data.getStoreId())
                    .orElseThrow(() -> APIException.notFound("Store with ID {} not found", data.getStoreId()));
            point.setStore(store);
        }

        ServicePoint savedPoint = repository.save(point);

        return savedPoint.toData();
    }
    public ServicePoint getServicePointByName(String name){
         switch (name.toUpperCase()){
             case "LABORATORY":
                 return getServicePointByType(ServicePointType.Laboratory);
             case "RADIOLOGY":
                 return getServicePointByType(ServicePointType.Radiology);
             case "PROCEDURE":
                 return getServicePointByType(ServicePointType.Procedure);
             default:
                 return null;
         }
    }
}
