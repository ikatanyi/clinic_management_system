/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.admission.domain.repository;

import io.smarthealth.clinical.admission.domain.CareTeam;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Simon.waweru
 */
public interface CareTeamRepository extends JpaRepository<CareTeam, Long> {
    
}
