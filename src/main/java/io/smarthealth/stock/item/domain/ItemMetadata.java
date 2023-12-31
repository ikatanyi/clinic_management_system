package io.smarthealth.stock.item.domain;

import io.smarthealth.accounting.taxes.domain.Tax;
import io.smarthealth.administration.servicepoint.data.SimpleServicePoint;
import io.smarthealth.stock.item.data.Uoms;
import io.smarthealth.stock.item.domain.enumeration.ItemCategory;
import io.smarthealth.stock.stores.data.StoreData;
import lombok.Data;

import java.util.List;

/**
 *
 * @author Kelsas
 */ 
@Data
public class ItemMetadata {

    private String code;
    private String message;
    private List<Uoms> uom;
    private List<Tax> taxes;
    private List<StoreData> stores; 
    private List<SimpleServicePoint> servicePoints;
    private List<ItemCategory> categories;
}
