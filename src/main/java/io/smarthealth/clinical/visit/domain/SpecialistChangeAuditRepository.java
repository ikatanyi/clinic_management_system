/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.visit.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author kennedy.Ikatanyi
 */
public interface SpecialistChangeAuditRepository extends JpaRepository<SpecialistChangeAudit, Long>, JpaSpecificationExecutor<SpecialistChangeAudit>, ResultsRepository {

}
