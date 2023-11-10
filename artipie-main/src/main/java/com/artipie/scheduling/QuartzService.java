/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scheduling;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Start quarts scheduling service.
 * @since 1.3
 */
public final class QuartzService {

    /**
     * Quartz scheduler.
     */
    private final Scheduler scheduler;

    /**
     * Ctor.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public QuartzService() {
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            QuartzService.this.scheduler.shutdown();
                        } catch (final SchedulerException error) {
                            Logger.error(this, error.getMessage());
                        }
                    }
                }
            );
        } catch (final SchedulerException error) {
            throw new ArtipieException(error);
        }
    }

    /**
     * Adds event processor to the quarts job. The job is repeating forever every
     * given seconds. Jobs are run in parallel, if several consumers are passed, consumer for job.
     * If consumers amount is bigger than thread pool size, parallel jobs mode is
     * limited to thread pool size.
     * @param seconds Seconds interval for scheduling
     * @param consumer How to consume the data for each job
     * @param <T> Data item object type
     * @return Queue to add the events into
     * @throws SchedulerException On error
     */
    public <T> Queue<T> addPeriodicEventsProcessor(
        final int seconds, final List<Consumer<T>> consumer) throws SchedulerException {
        final Queue<T> queue = new ConcurrentLinkedDeque<>();
        final String id = String.join(
            "-", EventsProcessor.class.getSimpleName(), UUID.randomUUID().toString()
        );
        final TriggerBuilder<SimpleTrigger> trigger = TriggerBuilder.newTrigger()
            .startNow().withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(seconds));
        final int count = this.parallelJobs(consumer.size());
        for (int item = 0; item < count; item = item + 1) {
            final JobDataMap data = new JobDataMap();
            data.put("elements", queue);
            data.put("action", Objects.requireNonNull(consumer.get(item)));
            this.scheduler.scheduleJob(
                JobBuilder.newJob(EventsProcessor.class).setJobData(data).withIdentity(
                    QuartzService.jobId(id, item), EventsProcessor.class.getSimpleName()
                ).build(),
                trigger.withIdentity(
                    QuartzService.triggerId(id, item),
                    EventsProcessor.class.getSimpleName()
                ).build()
            );
        }
        this.log(count, EventsProcessor.class.getSimpleName(), seconds);
        return queue;
    }

    /**
     * Schedule jobs for class `clazz` to be performed every `seconds` in parallel amount of
     * `thread` with given `data`. If scheduler thread pool size is smaller than `thread` value,
     * parallel jobs amount is reduced to thread pool size.
     * @param seconds Interval in seconds
     * @param threads Parallel threads amount
     * @param clazz Job class, implementation of {@link org.quartz.Job}
     * @param data Job data map
     * @param <T> Class type parameter
     * @return Set of the started quartz job keys
     * @throws SchedulerException On error
     * @checkstyle ParameterNumberCheck (7 lines)
     */
    public <T extends Job> Set<JobKey> schedulePeriodicJob(
        final int seconds, final int threads, final Class<T> clazz, final JobDataMap data
    ) throws SchedulerException {
        final String id = String.join(
            "-", clazz.getSimpleName(), UUID.randomUUID().toString()
        );
        final TriggerBuilder<SimpleTrigger> trigger = TriggerBuilder.newTrigger()
            .startNow().withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(seconds));
        final int count = this.parallelJobs(threads);
        final Set<JobKey> res = new HashSet<>(count);
        for (int item = 0; item < count; item = item + 1) {
            final JobKey key = new JobKey(QuartzService.jobId(id, item), clazz.getSimpleName());
            this.scheduler.scheduleJob(
                JobBuilder.newJob(clazz).setJobData(data).withIdentity(key).build(),
                trigger.withIdentity(
                    QuartzService.triggerId(id, item),
                    clazz.getSimpleName()
                ).build()
            );
            res.add(key);
        }
        this.log(count, clazz.getSimpleName(), seconds);
        return res;
    }

    /**
     * Schedule jobs for class `clazz` to be performed according to `cronexp` cron format schedule.
     * @param cronexp Cron expression in format {@link org.quartz.CronExpression}
     * @param clazz Class of the Job.
     * @param data JobDataMap for job.
     * @param <T> Class type parameter.
     * @throws SchedulerException On error.
     */
    public <T extends Job> void schedulePeriodicJob(
        final String cronexp, final Class<T> clazz, final JobDataMap data
    ) throws SchedulerException {
        final JobDetail job = JobBuilder
            .newJob()
            .ofType(clazz)
            .withIdentity(String.format("%s-%s", cronexp, clazz.getCanonicalName()))
            .setJobData(data)
            .build();
        final Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(
                String.format("trigger-%s", job.getKey()),
                "cron-group"
            )
            .withSchedule(CronScheduleBuilder.cronSchedule(cronexp))
            .forJob(job)
            .build();
        this.scheduler.scheduleJob(job, trigger);
    }

    /**
     * Delete quartz job by key.
     * @param key Job key
     */
    public void deleteJob(final JobKey key) {
        try {
            this.scheduler.deleteJob(key);
        } catch (final SchedulerException err) {
            Logger.error(
                this, "Error while deleting quartz job %s:\n%s", key.toString(), err.getMessage()
            );
        }
    }

    /**
     * Start quartz.
     */
    public void start() {
        try {
            this.scheduler.start();
        } catch (final SchedulerException error) {
            throw new ArtipieException(error);
        }
    }

    /**
     * Stop scheduler.
     */
    public void stop() {
        try {
            this.scheduler.shutdown(true);
        } catch (final SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Checks if scheduler thread pool size allows to handle given `requested` amount
     * of parallel jobs. If thread pool size is smaller than `requested` value,
     * warning is logged and the smallest value is returned.
     * @param requested Requested amount of parallel jobs
     * @return The minimum of requested value and thread pool size
     * @throws SchedulerException On error
     */
    private int parallelJobs(final int requested) throws SchedulerException {
        final int count = Math.min(
            this.scheduler.getMetaData().getThreadPoolSize(), requested
        );
        if (requested > count) {
            // @checkstyle LineLengthCheck (1 line)
            Logger.warn(this, String.format("Parallel quartz jobs amount is limited to thread pool size %s instead of requested %s", count, requested));
        }
        return count;
    }

    /**
     * Log info about started job.
     * @param count Parallel count
     * @param clazz Job class name
     * @param seconds Scheduled interval
     */
    private void log(final int count, final String clazz, final int seconds) {
        Logger.debug(
            this,
            String.format(
                "%s parallel %s jobs were scheduled to run every %s seconds", count, clazz, seconds
            )
        );
    }

    /**
     * Construct job id.
     * @param id Id
     * @param item Job number
     * @return Full job id
     */
    private static String jobId(final String id, final int item) {
        return String.join("-", "job", id, String.valueOf(item));
    }

    /**
     * Construct trigger id.
     * @param id Id
     * @param item Job number
     * @return Full trigger id
     */
    private static String triggerId(final String id, final int item) {
        return String.join("-", "trigger", id, String.valueOf(item));
    }
}
