package io.smarthealth.sequence;

import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * Sequence Number Repository.
 */
//@RepositoryRestResource(collectionResourceRel = "sequence", path = "sequence")
interface SequenceNumberRepository extends PagingAndSortingRepository<SequenceNumber, Long> {

    Iterable<SequenceNumber> findByDefinition(SequenceDefinition definition);

    SequenceNumber findByDefinitionAndGroupIsNull(SequenceDefinition definition);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    SequenceNumber findByIds(Long id);
}
