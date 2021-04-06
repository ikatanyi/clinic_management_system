package io.smarthealth.clinical.laboratory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabEquipmentRepository extends JpaRepository<LabEquipment, Long> {

    List<LabEquipment> findByEquipmentNameContaining(String equipmentName);

}
