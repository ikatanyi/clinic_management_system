/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.organization.person.data;

import lombok.Data;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author kelvin.sasaka
 */
@Embeddable
@Data
public class BiometricData implements Serializable {

//    @NotNull
//    @JsonProperty(value = "data_type")
//    private Biometric.Format format;
    @NotNull
    protected String data;
    protected String indicator;
}
