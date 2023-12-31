package io.smarthealth.stock.inventory.data;

import io.smarthealth.stock.item.domain.Item;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Balance Transaction Line of a given {@link Item } . It holds the current
 * balance information
 *
 * @author Kelsas
 */
@Data
public class InventoryItemData {

    private Long id;

    private Long storeId;
    private String storeName;

    private Long itemId;
    private String item;
    private String itemCode;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Double reorderLevel;
    private boolean isDrug;
    private Double availableStock;
    private BigDecimal stockValue;

    public BigDecimal getStockValue() {
        BigDecimal qty = BigDecimal.valueOf(availableStock);
        stockValue = qty.multiply(costPrice);
        return stockValue;
    }
}
