/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.artipie.adapters.conda.CondaQuartz;
import java.util.function.Predicate;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Repository quartz jobs schedulers.
 * @since 0.23
 */
public enum QuartzRepoJob implements Predicate<RepoConfig> {

    /**
     * Schedule job for anaconda repository.
     */
    CONDA {

        @Override
        public boolean test(final RepoConfig cnfg) {
            return cnfg.type().equals("conda");
        }

        @Override
        public void schedule(final RepoConfig config, final Scheduler scheduler)
            throws SchedulerException {
            new CondaQuartz(config).triggerJob(scheduler);
        }
    };

    /**
     * Schedule quartz job.
     * @param config Repository config
     * @param scheduler Quartz scheduler
     * @throws SchedulerException On error
     */
    public abstract void schedule(RepoConfig config, Scheduler scheduler)
        throws SchedulerException;
}
