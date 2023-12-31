/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.record.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author Simon.Waweru
 */
public interface DiseaseRepository extends JpaRepository<Disease, Long>{
    
    Page<Disease> findByNameContainingOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);

    Optional<Disease> findByCode(String code);
}
