/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.RepoConfig;
import com.artipie.conda.asto.AuthTokensMaid;
import com.jcabi.log.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

/**
 * Quartz job to clean conda auth tokens.
 * @since 0.23
 */
public final class CondaQuartz {

    /**
     * Repository configuration.
     */
    private final RepoConfig config;

    /**
     * Ctor.
     * @param config Repository configuration
     */
    public CondaQuartz(final RepoConfig config) {
        this.config = config;
    }

    /**
     * Create and trigger conda job.
     * @param scheduler Quartz scheduler
     * @throws SchedulerException On error while scheduling the job
     */
    public void triggerJob(final Scheduler scheduler) throws SchedulerException {
        final String group = "conda_group";
        scheduler.scheduleJob(
            JobBuilder.newJob(CondaJob.class)
                .withIdentity(String.format("conda_%s_job", this.config.name()), group).build(),
            TriggerBuilder.newTrigger()
                .withIdentity(String.format("conda_%s_trigger", this.config.name()), group)
                .withSchedule(
                    CronScheduleBuilder.cronSchedule(
                        new CondaConfig(this.config.settings()).cleanAuthTokens()
                    )
                ).build()
        );
        Logger.debug(this, "Job for repository %s scheduled", this.config.name());
    }

    /**
     * Quartz job implementation for conda.
     * @since 0.23
     */
    private final class CondaJob implements Job {

        @Override
        public void execute(final JobExecutionContext context) {
            new AuthTokensMaid(CondaQuartz.this.config.storage()).clean()
                .toCompletableFuture().join();
        }
    }
}
