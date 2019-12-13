package io.smarthealth.organization.org.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 *
 * @author Kelsas
 * @param <T> Entity
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByCode(String code);
    
    Optional<Organization> findTopByOrderByOrganizationNameDesc();
}
