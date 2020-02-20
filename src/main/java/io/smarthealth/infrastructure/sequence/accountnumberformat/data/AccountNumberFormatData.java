/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.infrastructure.sequence.accountnumberformat.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map; 

@Deprecated
public class AccountNumberFormatData implements Serializable {

    private final Long id;

    private final EnumOptionData accountType;
    private final EnumOptionData prefixType;

    // template options
    private List<EnumOptionData> accountTypeOptions;
    private Map<String, List<EnumOptionData>> prefixTypeOptions;

    public AccountNumberFormatData(final Long id, final EnumOptionData accountType, final EnumOptionData prefixType) {
        this(id, accountType, prefixType, null, null);
    }

    public AccountNumberFormatData(final List<EnumOptionData> accountTypeOptions, Map<String, List<EnumOptionData>> prefixTypeOptions) {
        this(null, null, null, accountTypeOptions, prefixTypeOptions);
    }

    public void templateOnTop(List<EnumOptionData> accountTypeOptions, Map<String, List<EnumOptionData>> prefixTypeOptions) {
        this.accountTypeOptions = accountTypeOptions;
        this.prefixTypeOptions = prefixTypeOptions;
    }

    private AccountNumberFormatData(final Long id, final EnumOptionData accountType, final EnumOptionData prefixType,
            final List<EnumOptionData> accountTypeOptions, Map<String, List<EnumOptionData>> prefixTypeOptions) {
        this.id = id;
        this.accountType = accountType;
        this.prefixType = prefixType;
        this.accountTypeOptions = accountTypeOptions;
        this.prefixTypeOptions = prefixTypeOptions;
    }

    public Long getId() {
        return this.id;
    }

    public EnumOptionData getAccountType() {
        return this.accountType;
    }

    public EnumOptionData getPrefixType() {
        return this.prefixType;
    }

    public List<EnumOptionData> getAccountTypeOptions() {
        return this.accountTypeOptions;
    }

    public Map<String, List<EnumOptionData>> getPrefixTypeOptions() {
        return this.prefixTypeOptions;
    }

}
