package io.smarthealth.infrastructure.jobs.service;

import io.smarthealth.infrastructure.jobs.domain.ScheduledJobDetail;
import io.smarthealth.infrastructure.jobs.domain.ScheduledJobRunHistory;
import io.smarthealth.infrastructure.utility.DateUtility;
import java.time.LocalDateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Trigger;
import org.springframework.stereotype.Component;

/**
 *
 * @author Kelsas
 */
@Component
public class SchedulerJobListener implements JobListener {

    private final String name = SchedulerServiceConstants.DEFAULT_LISTENER_NAME;
    private int stackTraceLevel = 0;

    private final SchedulerService schedularService;

    public SchedulerJobListener(SchedulerService schedularService) {
        this.schedularService = schedularService;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jec) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        final Trigger trigger = context.getTrigger();
        final JobKey key = context.getJobDetail().getKey();
        final String jobKey = key.getName() + SchedulerServiceConstants.JOB_KEY_SEPERATOR + key.getGroup();
        final ScheduledJobDetail scheduledJobDetails = this.schedularService.findByJobKey(jobKey);
        final Long version = this.schedularService.fetchMaxVersionBy(jobKey) + 1;
        String status = SchedulerServiceConstants.STATUS_SUCCESS;
        String errorMessage = null;
        String errorLog = null;
        if (jobException != null) {
            status = SchedulerServiceConstants.STATUS_FAILED;
            this.stackTraceLevel = 0;
            final Throwable throwable = getCauseFromException(jobException);
            this.stackTraceLevel = 0;
            StackTraceElement[] stackTraceElements = null;
            errorMessage = throwable.getMessage();
            stackTraceElements = throwable.getStackTrace();
            final StringBuilder sb = new StringBuilder(throwable.toString());
            for (final StackTraceElement element : stackTraceElements) {
                sb.append("\n \t at ").append(element.getClassName()).append(".").append(element.getMethodName()).append("(")
                        .append(element.getLineNumber()).append(")");
            }
            errorLog = sb.toString();

        }
        String triggerType = SchedulerServiceConstants.TRIGGER_TYPE_CRON;
        if (context.getMergedJobDataMap().containsKey(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE)) {
            triggerType = context.getMergedJobDataMap().getString(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE);
        }
        if ((triggerType == null ? SchedulerServiceConstants.TRIGGER_TYPE_CRON == null : triggerType.equals(SchedulerServiceConstants.TRIGGER_TYPE_CRON)) && trigger.getNextFireTime() != null
                && trigger.getNextFireTime().after(DateUtility.toDateTime(scheduledJobDetails.getNextRunTime()))) {
            scheduledJobDetails.setNextRunTime(DateUtility.toLocalDateTime(trigger.getNextFireTime()));
        }

        scheduledJobDetails.setPreviousRunStartTime(DateUtility.toLocalDateTime(context.getFireTime()));
        scheduledJobDetails.setCurrentlyRunning(false);

        final ScheduledJobRunHistory runHistory = new ScheduledJobRunHistory(scheduledJobDetails, version, DateUtility.toLocalDateTime(context.getFireTime()),
                LocalDateTime.now(), status, errorMessage, triggerType, errorLog);
        // scheduledJobDetails.addRunHistory(runHistory);

        this.schedularService.saveOrUpdate(scheduledJobDetails, runHistory);
    }

    private Throwable getCauseFromException(final Throwable exception) {
        if (this.stackTraceLevel <= SchedulerServiceConstants.STACK_TRACE_LEVEL
                && exception.getCause() != null
                && (exception.getCause().toString().contains(SchedulerServiceConstants.SCHEDULER_EXCEPTION)
                || exception.getCause().toString().contains(SchedulerServiceConstants.JOB_EXECUTION_EXCEPTION) || exception
                .getCause().toString().contains(SchedulerServiceConstants.JOB_METHOD_INVOCATION_FAILED_EXCEPTION))) {
            this.stackTraceLevel++;
            return getCauseFromException(exception.getCause());
        } else if (exception.getCause() != null) {
            return exception.getCause();
        }
        return exception;
    }
}
