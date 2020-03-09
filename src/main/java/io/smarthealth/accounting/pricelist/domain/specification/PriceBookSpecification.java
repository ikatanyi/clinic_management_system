package io.smarthealth.accounting.pricelist.domain.specification;

import io.smarthealth.accounting.pricelist.domain.PriceBook;
import io.smarthealth.accounting.pricelist.domain.enumeration.PriceCategory;
import io.smarthealth.accounting.pricelist.domain.enumeration.PriceType;
import java.util.ArrayList;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for searching the data and filtering for the user
 *
 * @author Kelsas
 */
public class PriceBookSpecification {

    public PriceBookSpecification() {
        super();
    }

    public static Specification<PriceBook> createSpecification(PriceCategory category, PriceType type, boolean includeClosed) {
        return (root, query, cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();

            if (!includeClosed) {
                predicates.add(cb.equal(root.get("active"), true));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("priceType"), type));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("priceCategory"), category));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}