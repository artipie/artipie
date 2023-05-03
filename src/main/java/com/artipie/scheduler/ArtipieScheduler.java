/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.scheduler;

import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;
import com.artipie.scripting.ScriptRunner;
import com.artipie.settings.Settings;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.jcabi.log.Logger;
import java.util.stream.Collectors;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Scheduler.
 * @since 0.30
 */
public final class ArtipieScheduler {
    /**
     * Scheduler.
     */
    private Scheduler scheduler;

    /**
     * Start scheduler.
     */
    public void start() {
        try {
            final StdSchedulerFactory factory = new StdSchedulerFactory();
            this.scheduler = factory.getScheduler();
            this.scheduler.start();
        } catch (final SchedulerException exc) {
            throw new ArtipieException(exc);
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
     * Schedule job.
     * Examples of cron expressions:
     * <ul>
     *     <li>"0 25 11 * * ?" means "11:25am every day"</li>
     *     <li>"0 0 11-15 * * ?" means "11AM and 3PM every day"</li>
     *     <li>"0 0 11-15 * * SAT-SUN" means "between 11AM and 3PM on weekends SAT-SUN"</li>
     * </ul>
     * @param job Job details
     * @param cronexp Cron expression in format {@link org.quartz.CronExpression}
     */
    public void scheduleJob(final JobDetail job, final String cronexp) {
        try {
            final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(
                    String.format("trigger-%s", job.getKey()),
                    "cron-group"
                )
                .withSchedule(CronScheduleBuilder.cronSchedule(cronexp))
                .forJob(job)
                .build();
            this.scheduler.scheduleJob(job, trigger);
        } catch (final SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Clear all jobs and triggers.
     */
    public void clearAll() {
        try {
            this.scheduler.clear();
        } catch (final SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Cancel job.
     * @param job Job key
     */
    public void cancelJob(final JobKey job) {
        try {
            this.scheduler.deleteJob(job);
        } catch (final SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Loads crontab from settings.
     * Format is:
     * <pre>
     *     meta:
     *       crontab:
     *         - key: scripts/script1.groovy
     *           cronexp: * * 10 * * ?
     *         - key: scripts/script2.groovy
     *           cronexp: * * 11 * * ?
     * </pre>
     * @param settings Artipie settings
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void loadCrontab(final Settings settings) {
        final CronDefinition crondef =
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        final CronParser parser = new CronParser(crondef);
        settings.crontab()
            .map(
                crontab ->
                    crontab.values().stream()
                        .map(YamlNode::asMapping)
                        .map(
                            yaml -> {
                                final String key = yaml.string("key");
                                final String cronexp = yaml.string("cronexp");
                                boolean valid = false;
                                try {
                                    parser.parse(cronexp).validate();
                                    valid = true;
                                } catch (final IllegalArgumentException exc) {
                                    Logger.error(
                                        ArtipieScheduler.class,
                                        "Invalid cron expression %s %[exception]s",
                                        cronexp,
                                        exc
                                    );
                                }
                                if (valid) {
                                    final JobDataMap data = new JobDataMap();
                                    data.put("key", key);
                                    data.put("settings", settings);
                                    final JobDetail job = JobBuilder
                                        .newJob()
                                        .ofType(ScriptRunner.class)
                                        .withIdentity(String.format("%s %s", cronexp, key))
                                        .setJobData(data)
                                        .build();
                                    this.scheduleJob(job, cronexp);
                                }
                                return null;
                            })
                        .collect(Collectors.toList())
            );
    }
}
