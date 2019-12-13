package io.smarthealth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 *
 * @author Kelsas
 */
@Data
@Validated
@ConfigurationProperties(prefix = "io.smarthealth")
public class ApplicationProperties {

    /**
     * The base path where reports will be stored after compilation
     */
    @NotNull
    private Resource storageLocation;
    /**
     * The location of JasperReports source files
     */
    @NotNull
    private Resource reportLocation;

}
