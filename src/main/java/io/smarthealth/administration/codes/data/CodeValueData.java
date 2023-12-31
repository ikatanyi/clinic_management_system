package io.smarthealth.administration.codes.data;

import io.smarthealth.administration.codes.domain.Code;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Immutable data object represent code-value data in system.
 */
@Data
public class CodeValueData implements Serializable {

    private  Long id;
    @NotNull(message = "Code Type is Required")
    private Code code;
    private String codeValue;
    private int position;
    private boolean isActive; 
}
