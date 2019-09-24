/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.queue.domain;

import io.smarthealth.organization.facility.domain.Department;
import io.smarthealth.organization.person.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Simon.waweru
 */
@Repository
public interface PatientQueueRepository extends JpaRepository<PatientQueue, Long> {

    public Page<PatientQueue> findByDepartment(Department department, Pageable pageable);

    public Page<PatientQueue> findByPatient(Patient patient, Pageable pageable);
}
