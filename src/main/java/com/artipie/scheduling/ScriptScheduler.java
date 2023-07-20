/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.scheduling;

import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;
import com.artipie.scripting.ScriptRunner;
import com.artipie.settings.Settings;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.jcabi.log.Logger;
import java.util.Map;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;

/**
 * Scheduler for Artipie scripts.
 * @since 0.30
 */
public final class ScriptScheduler {

    /**
     * Quarts service for scheduling.
     */
    private final QuartsService service;

    /**
     * Initializes new instance of scheduler.
     */
    public ScriptScheduler() {
        this.service = new QuartsService();
    }

    /**
     * Start scheduler.
     */
    public void start() {
        this.service.start();
    }

    /**
     * Stop scheduler.
     */
    public void stop() {
        this.service.stop();
    }

    /**
     * Schedule job.
     * Examples of cron expressions:
     * <ul>
     *     <li>"0 25 11 * * ?" means "11:25am every day"</li>
     *     <li>"0 0 11-15 * * ?" means "11AM and 3PM every day"</li>
     *     <li>"0 0 11-15 * * SAT-SUN" means "between 11AM and 3PM on weekends SAT-SUN"</li>
     * </ul>
     * @param cronexp Cron expression in format {@link org.quartz.CronExpression}
     * @param clazz Class of the Job.
     * @param data Map Data for the job's JobDataMap.
     * @param <T> Class type parameter.
     */
    public <T extends Job> void scheduleJob(
        final String cronexp, final Class<T> clazz, final Map<String, Object> data
    ) {
        try {
            this.service.schedulePeriodicJob(cronexp, clazz, new JobDataMap(data));
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
            .ifPresent(
                crontab ->
                    crontab.values().stream()
                        .map(YamlNode::asMapping)
                        .forEach(
                            yaml -> {
                                final String key = yaml.string("key");
                                final String cronexp = yaml.string("cronexp");
                                boolean valid = false;
                                try {
                                    parser.parse(cronexp).validate();
                                    valid = true;
                                } catch (final IllegalArgumentException exc) {
                                    Logger.error(
                                        ScriptScheduler.class,
                                        "Invalid cron expression %s %[exception]s",
                                        cronexp,
                                        exc
                                    );
                                }
                                if (valid) {
                                    final JobDataMap data = new JobDataMap();
                                    data.put("key", key);
                                    data.put("settings", settings);
                                    try {
                                        this.service.schedulePeriodicJob(
                                            cronexp, ScriptRunner.class, data
                                        );
                                    } catch (final SchedulerException ex) {
                                        throw new ArtipieException(ex);
                                    }
                                }
                            })
            );
    }
}
