/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.repo.QuartzRepoJob;
import com.artipie.rpm.misc.UncheckedConsumer;
import com.jcabi.log.Logger;
import java.util.Collection;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Quartz scheduler holder: starts scheduler, schedules the jobs and adds hook to shutdown.
 * @since 0.23
 */
public final class QuartzScheduler {

    /**
     * Repository configurations.
     */
    private final Collection<RepoConfig> config;

    /**
     * Ctor.
     * @param config Repositories
     */
    public QuartzScheduler(final Collection<RepoConfig> config) {
        this.config = config;
    }

    /**
     * Start scheduler, add shutdown hook and schedule the jobs.
     */
    void start() {
        try {
            final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            Runtime.getRuntime().addShutdownHook(
                new Thread(
                    () -> {
                        try {
                            scheduler.shutdown();
                            Logger.debug(this, "QuartzScheduler is shut down");
                        } catch (final SchedulerException err) {
                            throw new ArtipieException(err);
                        }
                    }
                )
            );
            scheduler.start();
            for (final QuartzRepoJob item : QuartzRepoJob.values()) {
                this.config.stream().filter(item)
                    .forEach(new UncheckedConsumer<>(cnfg -> item.schedule(cnfg, scheduler)));
            }
        } catch (final SchedulerException err) {
            throw new ArtipieException(err);
        }
    }

}
