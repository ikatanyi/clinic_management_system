/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.organization.company.domain;

import io.smarthealth.organization.facility.domain.Facility;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 *
 * @author Kelsas
 */
public interface CompanyLogoRepository extends CrudRepository<CompanyLogo, Long>{
    Optional<CompanyLogo>findByFacility(Facility facility);
    
}
