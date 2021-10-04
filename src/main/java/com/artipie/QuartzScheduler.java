/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Storage;
import com.artipie.conda.CondaQuartz;
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
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Repository configurations.
     */
    private final Collection<RepoConfig> config;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param config Repositories
     */
    public QuartzScheduler(final Storage asto, final Collection<RepoConfig> config) {
        this.asto = asto;
        this.config = config;
    }

    /**
     * Start scheduler, add shutdown hook and schedule the jobs.
     */
    void setup() {
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
            this.config.stream().filter(repo -> repo.type().equals("conda"))
                .forEach(
                    new UncheckedConsumer<>(
                        conda -> new CondaQuartz(this.asto, conda).triggerJob(scheduler)
                    )
            );
        } catch (final SchedulerException err) {
            throw new ArtipieException(err);
        }
    }

}
