package io.smarthealth.financial.account.domain.specification;

import io.smarthealth.financial.account.data.AccountData;
import io.smarthealth.financial.account.domain.Account;
import io.smarthealth.financial.account.domain.enumeration.AccountType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for searching the data and filtering for the user
 *
 * @author Kelsas
 */
public class AccountSpecification {

    public AccountSpecification() {
        super();
    }

    public static Specification<Account> createSpecification(final boolean includeClosed, final String term, final String type) {
        return (root, query, cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();

            if (!includeClosed) {
                predicates.add(cb.equal(root.get("disabled"), false));
            }

            if (term != null) {
                final String likeExpression = "%" + term + "%";
                predicates.add(
                        cb.or(
                                cb.like(root.get("identifier"), likeExpression),
                                cb.like(root.get("name"), likeExpression)
                        )
                );
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), AccountType.valueOf(type)));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
