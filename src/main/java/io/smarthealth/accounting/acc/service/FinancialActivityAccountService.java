package io.smarthealth.accounting.acc.service;

import io.smarthealth.accounting.acc.data.ActivityAccount;
import io.smarthealth.accounting.acc.data.v1.FinancialActivity;
import io.smarthealth.accounting.acc.domain.AccountEntity;
import io.smarthealth.accounting.acc.domain.FinancialActivityAccount;
import io.smarthealth.accounting.acc.domain.FinancialActivityAccountRepository;
import io.smarthealth.infrastructure.exception.APIException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Kelsas
 */
@Service
public class FinancialActivityAccountService {

    private final FinancialActivityAccountRepository repository;
    private final AccountService accountServices;

    public FinancialActivityAccountService(FinancialActivityAccountRepository repository, AccountService accountServices) {
        this.repository = repository;
        this.accountServices = accountServices;
    }

    public FinancialActivityAccount createMapping(ActivityAccount activityAccount) {

        AccountEntity account = accountServices.findByAccountNumber(activityAccount.getAccountIdentifier())
                .orElseThrow(() -> APIException.notFound("Account {0} Not Found", activityAccount.getAccountIdentifier()));
        FinancialActivity activity = activityAccount.getActivity();
        validateActivityMapping(activity, account);

        return repository.save(new FinancialActivityAccount(activity, account));
    }

    public Page<ActivityAccount> getAllFinancialMapping(Pageable page) {
        return repository.findAll(page)
                .map(acc -> ActivityAccount.map(acc));

    }

    public FinancialActivityAccount updateFinancialActivity(Long id, ActivityAccount activity) {
        FinancialActivityAccount fa = getActivityById(id);
        AccountEntity account = accountServices.findByAccountNumber(activity.getAccountIdentifier())
                .orElseThrow(() -> APIException.notFound("Account {0} Not Found", activity.getAccountIdentifier()));

        if (fa.getAccount() != account) {
            fa.setAccount(account);
        }
        return repository.save(fa);
    }

    public FinancialActivityAccount getActivityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> APIException.notFound("Activity with Id {0} not found", id));
    }

    public FinancialActivityAccount getActivityByAccount(String accountNumber) {
        return getFinancialActivityAccount(accountNumber)
                .orElseThrow(() -> APIException.notFound("Activity with Id {0} not found", accountNumber));
    }

    public Optional<FinancialActivityAccount> getByTransactionType(FinancialActivity activity) {
        return repository.findByFinancialActivity(activity);

    }

    public Optional<FinancialActivityAccount> getFinancialActivityAccount(String accountNumber) {
        AccountEntity account = accountServices.findByAccountNumber(accountNumber)
                .orElseThrow(() -> APIException.notFound("Account {0} Not Found", accountNumber));

        return repository.findByAccount(account);
    }

    private void validateActivityMapping(FinancialActivity activity, AccountEntity account) {
        if (!activity.getAccountType().name().equals(account.getLedger().getType())) {
            String error = "Financial Activity {0}  can only be associated with a Ledger Account of Type {1} the provided Ledger Account {2} ({3})'  is not of the required type";
            throw APIException.badRequest(error, activity.getActivityName(), activity.getAccountType().name(), account.getName(), account.getIdentifier());
        }

    }

    public List<FinancialActivity> getActivities() {
        return Arrays.asList(FinancialActivity.values());
    }
}
