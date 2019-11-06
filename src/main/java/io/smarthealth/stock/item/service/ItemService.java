package io.smarthealth.stock.item.service;

import io.smarthealth.accounting.account.domain.Account;
import io.smarthealth.accounting.account.domain.AccountRepository;
import io.smarthealth.accounting.account.domain.enumeration.AccountCategory;
import io.smarthealth.accounting.taxes.domain.Tax;
import io.smarthealth.accounting.taxes.domain.TaxRepository;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.stock.inventory.domain.StockMovement;
import io.smarthealth.stock.item.data.CreateItem;
import io.smarthealth.stock.item.data.ItemData;
import io.smarthealth.stock.item.data.UomData;
import io.smarthealth.stock.item.data.Uoms;
import io.smarthealth.stock.item.domain.Drug;
import io.smarthealth.stock.item.domain.DrugRepository;
import io.smarthealth.stock.item.domain.Item;
import io.smarthealth.stock.item.domain.ItemMetadata;
import io.smarthealth.stock.item.domain.ItemRepository;
import io.smarthealth.stock.item.domain.ReorderRule;
import io.smarthealth.stock.item.domain.ReorderRuleRepository;
import io.smarthealth.stock.item.domain.Uom;
import io.smarthealth.stock.item.domain.UomRepository;
import io.smarthealth.stock.item.domain.specification.ItemSpecification;
import io.smarthealth.stock.stores.data.StoreData;
import io.smarthealth.stock.stores.domain.Store;
import io.smarthealth.stock.stores.domain.StoreRepository;
import io.smarthealth.stock.stores.service.StoreService;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Kelsas
 */
@Service
@Slf4j
public class ItemService {

    private final TaxRepository taxRepository;
    private final ItemRepository itemRepository;
    private final UomService uomService;
    private final StoreService storeService;
    private final ReorderRuleRepository reorderRuleRepository;
    private final DrugRepository drugRepository;

    public ItemService(
            TaxRepository taxRepository,
            ItemRepository itemRepository,
            UomService uomService,
            StoreService storeService,
            ReorderRuleRepository reorderRuleRepository,
            DrugRepository drugRepository) {
        this.taxRepository = taxRepository;
        this.itemRepository = itemRepository;
        this.uomService = uomService;
        this.storeService = storeService;
        this.reorderRuleRepository = reorderRuleRepository;
        this.drugRepository = drugRepository;
    }

    @Transactional
    public ItemData createItem(CreateItem createItem) {
        Item item = new Item();
        item.setActive(Boolean.TRUE);
        item.setCategory(createItem.getStockCategory());
        item.setCostRate(createItem.getPurchaseRate());
        item.setDescription(createItem.getDescription());
        item.setItemCode(createItem.getSku());
        item.setItemName(createItem.getItemName());
        item.setItemType(createItem.getItemType());
        item.setRate(createItem.getRate());
        //check the category
        if (createItem.getItemUnit() != null) {
            Optional<Uom> uom = uomService.findUomById(Long.valueOf(createItem.getItemUnit()));
            if (uom.isPresent()) {
                item.setUom(uom.get());
            }
        }
        if (createItem.getTaxId() != null) {
            Optional<Tax> tax = taxRepository.findById(createItem.getTaxId());
            if (tax.isPresent()) {
                item.setTax(tax.get());
            }
        }

        Item savedItem = itemRepository.save(item);

        if (createItem.getStockCategory().equals("Drugs")) {
            Drug drug = new Drug();
            drug.setItem(savedItem);
            drug.setDrugCategory(createItem.getDrugCategory());
            drug.setDrugForm(createItem.getDoseForm());
            drug.setRoute(createItem.getDrugRoute());
            drug.setStrength(createItem.getDrugStrength());
            drugRepository.save(drug);
        }

        if (createItem.getItemType().equals("Inventory")) {
            Store store = storeService.getStore(createItem.getInventoryStore())
                    .orElseThrow(() -> APIException.notFound("Store with Identifier {0} not found ", createItem.getInventoryStore()));

            if (createItem.getStockBalance() > 0) {
                log.info("Receiving the initial stock");
            }
            if (createItem.getReorderLevel() > 0) {
                ReorderRule rule = new ReorderRule();
                rule.setStore(store);
                rule.setStockItem(savedItem);
                rule.setReorderLevel(createItem.getReorderLevel());
                rule.setReorderQty(createItem.getOrderQuantity());
                reorderRuleRepository.save(rule);
            }
        }

        return ItemData.map(savedItem);
    }

    public Optional<Item> findById(final Long itemId) {
        return itemRepository.findById(itemId);
    }

    public Optional<Item> findByItemCode(final String itemCode) {
        return itemRepository.findByItemCode(itemCode);
    }

    public Item findByItemCodeOrThrow(final String itemCode) {
        return itemRepository.findByItemCode(itemCode).orElseThrow(() -> APIException.notFound("Item identified by code {0} not found", itemCode));
    }

    public Page<Item> fetchItems(final boolean includeClosed, String term, Pageable pageable) {
        Specification<Item> spec = ItemSpecification.createSpecification(includeClosed, term);
        Page<Item> items = itemRepository.findAll(spec, pageable);
        return items;
    }

    //this should be cached
    public ItemMetadata getItemMetadata() {
        List<StoreData> stores = storeService.getAllStores();
        List<Tax> tax = taxRepository.findAll();
        List<Uoms> uoms = uomService.getAllUnitofMeasure();

        ItemMetadata data = new ItemMetadata();
        data.setCode("0");
        data.setMessage("success");
        data.setStores(stores);
        data.setTaxes(tax);
        data.setUom(uoms);
        return data;
    }
}
