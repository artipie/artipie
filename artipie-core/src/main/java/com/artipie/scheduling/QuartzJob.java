/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Super class for classes, which implement {@link Job} interface.
 * The class has some common useful methods to avoid code duplication.
 * @since 1.3
 */
public abstract class QuartzJob implements Job {

    /**
     * Stop the job and log error.
     * @param context Job context
     */
    protected void stopJob(final JobExecutionContext context) {
        final JobKey key = context.getJobDetail().getKey();
        try {
            Logger.error(
                this,
                String.format(
                    "Events queue/action is null or EventProcessingError occurred, processing failed. Stopping job %s...", key
                )
            );
            new StdSchedulerFactory().getScheduler().deleteJob(key);
            Logger.error(this, String.format("Job %s stopped.", key));
        } catch (final SchedulerException error) {
            Logger.error(this, String.format("Error while stopping job %s", key));
            throw new ArtipieException(error);
        }
    }
}
