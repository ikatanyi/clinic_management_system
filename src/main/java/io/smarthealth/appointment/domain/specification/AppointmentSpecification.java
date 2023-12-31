package io.smarthealth.appointment.domain.specification;

import io.smarthealth.appointment.domain.Appointment;
import io.smarthealth.appointment.domain.enumeration.StatusType;
import io.smarthealth.infrastructure.lang.DateRange;
import java.util.ArrayList;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author Kelsas
 */
public class AppointmentSpecification {

    public AppointmentSpecification() {
        super();
    }

    public static Specification<Appointment> createSpecification(String staffNumber, String patientId, StatusType status, String deptCode, String urgency, String patientName, DateRange range) {
        return (root, query, cb) -> {
            final ArrayList<Predicate> predicates = new ArrayList<>();
 
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("patientId"), patientId));
            }
            if (staffNumber != null) {
                predicates.add(cb.equal(root.get("practitioner").get("staffNumber"), staffNumber));
            }
            if (deptCode != null) {
                predicates.add(cb.equal(root.get("department").get("code"), deptCode));
            }
            if (urgency != null) {
                predicates.add(cb.equal(root.get("urgency"), urgency));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (patientName != null) {
                final String likeExpression = "%" + patientName + "%";
                predicates.add(
                        cb.or(
                                cb.like(root.get("firstName"), likeExpression),
                                cb.like(root.get("lastName"), likeExpression)
                        )
                );
            }
             if(range!=null){
                  predicates.add(
                     cb.between(root.get("appointmentDate"), range.getStartDate(), range.getEndDate())
                  );
              }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
