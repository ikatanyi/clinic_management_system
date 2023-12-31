package io.smarthealth.clinical.admission.service;

import io.smarthealth.clinical.admission.data.BedChargeData;
import io.smarthealth.clinical.admission.data.BedTypeData;
import io.smarthealth.clinical.admission.domain.BedCharge;
import io.smarthealth.clinical.admission.domain.BedType;
import io.smarthealth.clinical.admission.domain.repository.BedChargeRepository;
import io.smarthealth.clinical.admission.domain.repository.BedTypeRepository;
import io.smarthealth.clinical.admission.domain.specification.BedChargeSpecification;
import io.smarthealth.clinical.admission.domain.specification.BedTypeSpecification;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.stock.item.domain.Item;
import io.smarthealth.stock.item.service.ItemService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author Kennedy.Ikatanyi
 */
@Service
@RequiredArgsConstructor
public class BedTypeService {

    private final BedTypeRepository bedTypeRepository;
    private final ItemService itemService;
    private final BedChargeRepository bedChargeRepository;

    public BedType createBedType(BedTypeData data) {
        BedType bedType = data.map();
        List<BedCharge> bedCharges = new ArrayList();
//        for (BedChargeData charge : data.getBedCharges()) {
//            BedCharge bedCharge = charge.map();
//            Item item = itemService.findItemEntityOrThrow(charge.getItemId());
//            bedCharge.setItem(item);
//            bedCharges.add(bedCharge);
//        }
//        bedType.addBedCharges(bedCharges);
        if (fetchBedTypeByName(data.getName()).isPresent()) {
            throw APIException.conflict("BedType {0} already exists.", data.getName());
        }
        return bedTypeRepository.save(bedType);
    }

    public Page<BedType> fetchAllBedTypes(Pageable page) {
        return bedTypeRepository.findAll(page);
    }

    public Page<BedType> fetchBedTypes(String name, Boolean active, String term, Pageable page) {
        Specification<BedType> spec = BedTypeSpecification.createSpecification(name, active, term);
        return bedTypeRepository.findAll(spec, page);
    }

    public Optional<BedType> fetchBedTypeByName(String name) {
        return bedTypeRepository.findByNameContainingIgnoreCase(name);
    }

    public BedType getBedType(Long id) {
        return bedTypeRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("BedType with id  {0} not found.", id));
    }

    public BedType updateBedType(Long id, BedTypeData data) {
        BedType bedType = getBedType(id);

        bedType.setName(data.getName());
        bedType.setDescription(data.getDescription());
        bedType.setIsActive(data.getActive());
        bedType.setName(data.getName());
        return bedTypeRepository.save(bedType);
    }

    public BedCharge createBedCharge(Long bedTypeId, BedChargeData data) {
        BedCharge bedCharge = null;
        if (data.getId() == null) {
            bedCharge = data.map();
        } else {
            bedCharge = getBedCharge(data.getId());
            bedCharge.setActive(data.getActive());
            bedCharge.setRate(data.getRate());
            bedCharge.setRecurrent(data.getRecurrent());
        }
        BedType bedType = getBedType(bedTypeId);
        Item item = itemService.findItemEntityOrThrow(data.getItemId());
        bedCharge.setItem(item);
        bedCharge.setBedType(bedType);
        return bedChargeRepository.save(bedCharge);
    }

    public List<BedCharge> createBatchBedCharge(Long bedTypeId, List<BedChargeData> list) {
        List<BedCharge> bedCharges = new ArrayList();
        list.stream().map((data) -> {
            BedCharge bedCharge = null;
            if (data.getId() == null) {
                bedCharge = data.map();
            } else {
                bedCharge = getBedCharge(data.getId());
                bedCharge.setActive(data.getActive());
                bedCharge.setRate(data.getRate());
                bedCharge.setRecurrent(data.getRecurrent());
            }
            BedType bedType = getBedType(bedTypeId);
            Item item = itemService.findItemEntityOrThrow(data.getItemId());
            bedCharge.setItem(item);
            bedCharge.setBedType(bedType);
            return bedCharge;
        }).forEachOrdered((bedCharge) -> {
            bedCharges.add(bedCharge);
        });
        return bedChargeRepository.saveAll(bedCharges);
    }

    public Page<BedCharge> fetchAllBedCharges(Long bedTypeId, String itemName, Pageable page) {
        Specification<BedCharge> spec = BedChargeSpecification.createSpecification(bedTypeId, itemName);
        return bedChargeRepository.findAll(spec, page);
    }

    public BedCharge getBedCharge(Long id) {
        return bedChargeRepository.findById(id)
                .orElseThrow(() -> APIException.notFound("BedCharge with id  {0} not found.", id));
    }

    public BedCharge updateBedCharge(Long id, BedChargeData data) {
        BedCharge bedCharge = getBedCharge(id);
        bedCharge.setActive(data.getActive());
        bedCharge.setRate(data.getRate());
        Item item = itemService.findItemEntityOrThrow(data.getItemId());
        bedCharge.setItem(item);
        return bedChargeRepository.save(bedCharge);
    }
}
