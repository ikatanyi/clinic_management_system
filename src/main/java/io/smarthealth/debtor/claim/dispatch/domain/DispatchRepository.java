/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.debtor.claim.dispatch.domain;

import io.smarthealth.debtor.claim.creditNote.domain.*;
import io.smarthealth.debtor.claim.allocation.domain.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author Kennedy.Imbenzi
 */
public interface DispatchRepository extends JpaRepository<Dispatch, Long>, JpaSpecificationExecutor<Dispatch>{
    Optional<Dispatch>findByDispatchNo(String dispatchNo);
}
