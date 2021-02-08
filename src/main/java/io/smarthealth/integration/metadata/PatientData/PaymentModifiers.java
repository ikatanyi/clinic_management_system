/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.integration.metadata.PatientData;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
@JacksonXmlRootElement(localName="Payment_modifiers")
public class PaymentModifiers {
    @JsonProperty("Payment_Modifier")
    private PaymentModifier paymentModifier;
    @JsonProperty("PaymentModifier")
    private NHIFPaymentModifier nhifPaymentModifier;
}
