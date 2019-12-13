package io.smarthealth.infrastructure.repository;

import io.smarthealth.infrastructure.domain.Identifiable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 *
 * @author Kelsas
 */
@NoRepositoryBean
public interface UuidRepository<T extends Identifiable> extends JpaRepository<T, Long> {

    Optional<T> findByUuid(String uuid);
}
