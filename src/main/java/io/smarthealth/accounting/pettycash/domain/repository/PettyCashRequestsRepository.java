/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.accounting.pettycash.domain.repository;

import io.smarthealth.accounting.pettycash.domain.PettyCashRequests;
import io.smarthealth.organization.facility.domain.Employee;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author Simon.waweru
 */
public interface PettyCashRequestsRepository extends JpaRepository<PettyCashRequests, Long>, JpaSpecificationExecutor<PettyCashRequests> {
    
    Optional<PettyCashRequests> findByRequestNo(String requestNo);
    
    Page<PettyCashRequests> findByApprovalPendingLevel(final int pendingLevel, final Pageable pageable);
    
    Page<PettyCashRequests> findByRequestedBy(final Employee employee, final Pageable pageable);
    
}
