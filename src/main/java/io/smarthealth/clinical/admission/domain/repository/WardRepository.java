/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.admission.domain.repository;

import io.smarthealth.clinical.admission.domain.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Kelsas
 */
public interface WardRepository extends JpaRepository<Ward, Long> {

}