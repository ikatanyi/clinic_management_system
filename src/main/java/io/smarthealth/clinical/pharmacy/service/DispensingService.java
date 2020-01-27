package io.smarthealth.clinical.pharmacy.service;

import io.smarthealth.accounting.billing.domain.PatientBill;
import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.accounting.billing.service.BillingService;
import io.smarthealth.clinical.pharmacy.data.PharmacyData;
import io.smarthealth.clinical.pharmacy.domain.DispensedDrug;
import io.smarthealth.infrastructure.common.SecurityUtils;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.stock.inventory.domain.StockEntry;
import io.smarthealth.stock.inventory.service.InventoryService;
import io.smarthealth.stock.item.domain.Item;
import io.smarthealth.stock.stores.domain.Store;
import io.smarthealth.stock.stores.service.StoreService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.smarthealth.clinical.pharmacy.domain.DispensedDrugRepository;
import io.smarthealth.clinical.pharmacy.domain.specification.DispensingSpecification;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.numbers.service.SequenceNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author Kelsas
 */
@Service
@RequiredArgsConstructor
public class DispensingService {

    private final DispensedDrugRepository repository;
    private final PatientService patientService;
    private final StoreService storeService;
    private final BillingService billingService;
    private final InventoryService inventoryService;
    private final SequenceNumberGenerator sequenceGenerator;

    public String dispenseDrug(PharmacyData patientDrugs) {;
        String trdId = sequenceGenerator.generateTransactionNumber();

        Patient patient = patientService.findPatientOrThrow(patientDrugs.getPatientNumber());
        Store store = storeService.getStoreWithNoFoundDetection(patientDrugs.getStoreId());

        List<DispensedDrug> dispensedlist = new ArrayList<>();

        if (!patientDrugs.getDrugItems().isEmpty()) {
            patientDrugs.getDrugItems()
                    .stream()
                    .forEach(drugData -> {
                        DispensedDrug drugs = new DispensedDrug();
                        Item item = billingService.getItemByCode(drugData.getItemCode());
                        drugs.setPatient(patient);
                        drugs.setDrug(item);
                        drugs.setStore(store);

                        drugs.setDispensedDate(patientDrugs.getDispenseDate());
                        drugs.setTransactionId(trdId);
                        drugs.setQtyIssued(drugData.getQuantity());
                        drugs.setPrice(drugData.getPrice());
                        drugs.setAmount(drugData.getAmount());
                        drugs.setUnits(drugData.getUnit());
                        drugs.setDoctorName(drugData.getDoctorName());
                        drugs.setPaid(false);
                        drugs.setCollected(true);
                        drugs.setDispensedBy(SecurityUtils.getCurrentUserLogin().orElse(""));
                        drugs.setCollectedBy("");

                        drugs.setInstructions(drugData.getInstructions());
                        dispensedlist.add(drugs);
                    });
        }

        // dispense drug
        List<DispensedDrug> savedList = repository.saveAll(dispensedlist);

        List<StockEntry> stockEntries = savedList.stream()
                .map(drug -> StockEntry.create(drug))
                .collect(Collectors.toList());
        //affect stocks
        inventoryService.saveAll(stockEntries);
        //billing
        PatientBill savedBill = billingService.createBill(store, patientDrugs);
        //aftect journals
        return savedBill.getTransactionId();
    }

    public DispensedDrug findDispensedDrugOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> APIException.notFound("Dispensed Drug with id {0} not found", id));
    }

    public Page<DispensedDrug> findDispensedDrugs(String transactionNo, String visitNo, String patientNo, String prescriptionNo, String billNo, String status, Pageable page) {
        BillStatus state = BillStatus.valueOf(status);
        Specification<DispensedDrug> spec = DispensingSpecification.createSpecification(transactionNo, visitNo, patientNo, prescriptionNo, billNo, state);

        return repository.findAll(spec, page);

    }
}
