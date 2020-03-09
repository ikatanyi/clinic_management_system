package io.smarthealth.accounting.accounts.data; 
import io.smarthealth.accounting.accounts.domain.FinancialActivityAccount;
import lombok.Value;

/**
 *
 * @author Kelsas
 */ 
 @Value
public class ActivityAccount {
    private Long id;
    private FinancialActivity activity;
    private String activityName;
    private String accountName;
    private String accountIdentifier;

    public static ActivityAccount map(FinancialActivityAccount account) {
        return new ActivityAccount(
                account.getId(),
                account.getFinancialActivity(),
                account.getFinancialActivity().getActivityName(),
                account.getAccount().getName(),
                account.getAccount().getIdentifier()
        );
    }
}
