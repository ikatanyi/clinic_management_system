package io.smarthealth.administration.servicepoint.service;

import io.smarthealth.accounting.acc.domain.AccountEntity;
import io.smarthealth.accounting.acc.service.AccountService;
import io.smarthealth.administration.servicepoint.data.ServicePointData;
import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.infrastructure.exception.APIException;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.smarthealth.administration.servicepoint.domain.ServicePointRepository;
import java.util.Optional;

/**
 *
 * @author Kelsas
 */
@Service
public class ServicePointService {

    private final ServicePointRepository repository;
    private final AccountService accountService;

    public ServicePointService(ServicePointRepository repository, AccountService accountService) {
        this.repository = repository;
        this.accountService = accountService;
    }

    public ServicePointData createPoint(ServicePointData data) {
        ServicePoint point = new ServicePoint();
        point.setActive(data.getActive());
        point.setDescription(data.getDescription());
        point.setName(data.getName());
        point.setPointType(data.getPointType());
        point.setServicePointType(data.getServicePointType());
        if (data.getExpenseAccount() != null && data.getExpenseAccount().getAccountNumber() != null) {
            AccountEntity acc = accountService.findOneWithNotFoundDetection(data.getExpenseAccount().getAccountNumber());
            point.setExpenseAccount(acc);
        }

        if (data.getIncomeAccount() != null && data.getIncomeAccount().getAccountNumber() != null) {
            AccountEntity acc = accountService.findOneWithNotFoundDetection(data.getIncomeAccount().getAccountNumber());
            point.setIncomeAccount(acc);
        }

        ServicePoint savedPoint = repository.save(point);
        return savedPoint.toData();
    }

    public ServicePoint getServicePoint(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> APIException.notFound("Service point with id {0} not found", id));
    }

    public ServicePoint getServicePointByType(final ServicePointType servicePointType) {
        return repository
                .findByServicePointType(servicePointType)
                .orElseThrow(() -> APIException.notFound("Service point identified by  {0} not found", servicePointType.name()));
    }

    public Optional<ServicePoint> getServicePoint(final ServicePointType servicePointType) {
        return repository
                .findByServicePointType(servicePointType);
    }

    public Page<ServicePointData> listServicePoints(String pointType, Pageable page) {
        Page<ServicePoint> list;
        if (pointType != null) {
            list = repository.findByPointType(pointType, page);
        } else {
            list = repository.findAll(page);
        }
        return list
                .map(sd -> sd.toData());
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
        ServicePoint savedPoint = repository.save(point);

        return savedPoint.toData();
    }
}
