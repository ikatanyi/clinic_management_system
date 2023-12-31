/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.administration.config.data;

import io.smarthealth.administration.config.domain.ExternalService;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Kelsas
 */
@Getter
@Setter
public class ExternalServicesData {

    private final Long id;
    private final String name;
 

    public ExternalServicesData(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public static ExternalServicesData of(ExternalService externalService) {
        return new ExternalServicesData(externalService.getId(), externalService.getName());
    }
}
