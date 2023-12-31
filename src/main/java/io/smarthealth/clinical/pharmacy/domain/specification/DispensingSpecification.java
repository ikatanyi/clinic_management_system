package io.smarthealth.clinical.pharmacy.domain.specification;

import io.smarthealth.accounting.billing.domain.enumeration.BillStatus;
import io.smarthealth.clinical.pharmacy.domain.DispensedDrug;
import io.smarthealth.infrastructure.lang.DateRange;

import java.util.ArrayList;
import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for searching the data and filtering for the user
 *
 * @author Kelsas
 */
public class DispensingSpecification {

    public DispensingSpecification() {
        super();
    }

    public static Specification<DispensedDrug> createSpecification(String refNo, String visitNo, String patientNo,
                                                                   String prescription, String billNo, Boolean isReturn,
                                                                   DateRange range) {
        return (root, query, cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();
            if (refNo != null) {
                predicates.add(cb.equal(root.get("transaction_id"), refNo));
            }

            if (visitNo != null) {
                predicates.add(cb.equal(root.get("visit").get("visitNumber"), visitNo));
            }

            if (patientNo != null) {
                predicates.add(cb.equal(root.get("patient").get("patientNumber"), patientNo));
            }

            if (prescription != null) {
                predicates.add(cb.equal(root.get("prescription").get("orderNumber"), prescription));
            }

            if (billNo != null) {
                predicates.add(cb.equal(root.get("billNumber"), billNo));
            }
            if (isReturn != null) {
                predicates.add(cb.equal(root.get("isReturn"), isReturn));
            }
            if (range != null) {
                predicates.add(
                        cb.between(root.get("dispensedDate"), range.getStartDate(), range.getEndDate())
                );
            }

//            if (term != null) {
//                final String likeExpression = "%" + term + "%";
//                predicates.add(
//                        cb.or(
//                                cb.like(root.get("accountNumber"), likeExpression),
//                                cb.like(root.get("accountName"), likeExpression)
//                        )
//                );
//            }
//            if (type != null) {
//                predicates.add(cb.equal(root.get("accountType"), type));
//            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
