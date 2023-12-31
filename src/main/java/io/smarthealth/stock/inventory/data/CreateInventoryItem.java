package io.smarthealth.stock.inventory.data;

import io.smarthealth.stock.item.domain.Item;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Balance Transaction Line of a given {@link Item } . It holds the current
 * balance information
 *
 * @author Kelsas
 */
@Data
public class CreateInventoryItem {

    private Long storeId;
    private String storeName;

    private List<InventoriesItem> inventoryItems=new ArrayList<>();
}
