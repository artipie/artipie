/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.events;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.util.function.Consumer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Job to process events from queue.
 * Class type is used as quarts job type and is instantiated inside {@link org.quartz}, so
 * this class must have empty ctor. Events queue and action to consume the event are
 * set by {@link org.quartz} mechanism via setters. Note, that job instance is created by
 * {@link org.quartz} on every execution, but job data is not.
 * <a href="https://github.com/quartz-scheduler/quartz/blob/main/docs/tutorials/tutorial-lesson-02.md">Read more.</a>
 * @param <T> Elements type to process
 * @since 1.17
 */
public final class EventsProcessor<T> implements Job {

    /**
     * Elements.
     */
    private EventQueue<T> elements;

    /**
     * Action to perform on element.
     */
    private Consumer<T> action;

    @Override
    public void execute(final JobExecutionContext context) {
        if (this.action == null || this.elements == null) {
            this.stopJob(context);
        } else {
            while (!this.elements.queue().isEmpty()) {
                final T item = this.elements.queue().poll();
                if (item != null) {
                    this.action.accept(item);
                }
            }
        }
    }

    /**
     * Set elements queue from job context.
     * @param queue Queue with elements to process
     */
    public void setElements(final EventQueue<T> queue) {
        this.elements = queue;
    }

    /**
     * Set elements consumer from job context.
     * @param consumer Action to consume the element
     */
    public void setAction(final Consumer<T> consumer) {
        this.action = consumer;
    }

    /**
     * Stop the job and log error.
     * @param context Job context
     */
    private void stopJob(final JobExecutionContext context) {
        final JobKey key = context.getJobDetail().getKey();
        try {
            Logger.error(
                this,
                String.format(
                    "Events queue or action is null, processing failed. Stopping job %s...", key
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
