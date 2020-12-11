/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.messaging.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Data
public class DaEmail {

    private String from;
    private String mailTo;
    private String subject;
    private List<Object> attachments;
    private Map<String, Object> props;
    private String template;
    private boolean html;

    public DaEmail() {
    }

}
