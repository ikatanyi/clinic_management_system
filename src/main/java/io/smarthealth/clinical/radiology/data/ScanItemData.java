/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.radiology.data;

import lombok.Data;

/**
 *
 * @author Simon.Waweru
 */
@Data
public class ScanItemData {
    private Long medicId;
    private String itemCode;
    private double itemPrice;
    private int quantity;
}
