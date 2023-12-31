package io.smarthealth.stock.inventory.data;

import io.smarthealth.stock.item.domain.Item;
import lombok.Data;

/**
 * Balance Transaction Line of a given {@link Item } . It holds the current
 * balance information
 *
 * @author Kelsas
 */
@Data
public class InventoriesItem {

    private Long id;
    private Long itemId;
    private String item;
    private String itemCode;
    private double costPrice;
    private double sellingPrice;
    private double availableStock;
}
