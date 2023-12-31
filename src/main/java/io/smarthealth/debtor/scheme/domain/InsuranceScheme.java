package io.smarthealth.debtor.scheme.domain;

import io.smarthealth.debtor.payer.domain.Payer;
import io.smarthealth.debtor.scheme.domain.enumeration.PolicyCover;
import io.smarthealth.infrastructure.domain.Auditable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import lombok.Data;

/**
 *
 * @author Simon.Waweru
 */
@Data
@Entity
public class InsuranceScheme extends Auditable {

    @Column(nullable = false, unique = true)
    private String schemeName;
    @Enumerated(EnumType.STRING)
    private PolicyCover cover;
    private String category;

    @Column(nullable = false, unique = true)
    private String telNo;
    private String mobileNo;
    private String emailAddress;
    private String line1;
    private String line2;

    @ManyToOne
    private Payer payer;

}
