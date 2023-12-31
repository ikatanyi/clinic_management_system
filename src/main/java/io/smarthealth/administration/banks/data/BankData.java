/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.administration.banks.data;

import java.util.List;
import lombok.Data;

/**
 *
 * @author Simon.Waweru
 */
@Data
public class BankData {

    private Long bankId;
    private String bankName;
    private String bankCode;
    private String bankShortName;
    private List<BankBranchData> branch;

}
