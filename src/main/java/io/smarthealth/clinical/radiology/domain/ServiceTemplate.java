/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.radiology.domain;

import io.smarthealth.infrastructure.domain.Identifiable;
import io.smarthealth.organization.person.domain.enumeration.Gender;
import io.smarthealth.stock.item.domain.Item;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
@Entity
@Table(name = "service_template",uniqueConstraints = {
    @UniqueConstraint(columnNames = {"templateName"}, name="unique_template_name")})
@Inheritance(strategy = InheritanceType.JOINED)
public class ServiceTemplate extends Identifiable{
    private String templateName; 
    private Gender gender;
    private String notes;
}