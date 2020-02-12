/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.administration.config.domain;

import io.smarthealth.administration.config.data.enums.ApprovalModule;
import io.smarthealth.organization.facility.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Simon.waweru
 */
@Repository
public interface ModuleApproversRepository extends JpaRepository<ModuleApprovers, Long> {

    List<ModuleApprovers> findByModuleName(ApprovalModule moduleName);

    Optional<ModuleApprovers> findByModuleNameAndEmployee(final ApprovalModule moduleName, final Employee employee);
}