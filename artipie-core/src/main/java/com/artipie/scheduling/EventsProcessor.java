/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import com.jcabi.log.Logger;
import java.util.Queue;
import java.util.function.Consumer;
import org.quartz.JobExecutionContext;

/**
 * Job to process events from queue.
 * Class type is used as quarts job type and is instantiated inside {@link org.quartz}, so
 * this class must have empty ctor. Events queue and action to consume the event are
 * set by {@link org.quartz} mechanism via setters. Note, that job instance is created by
 * {@link org.quartz} on every execution, but job data is not.
 * <p/>
 * In the case of {@link EventProcessingError} processor tries to process the event three times,
 * if on the third time processing failed, job is shut down and event is not returned to queue.
 * <p/>
 * <a href="https://github.com/quartz-scheduler/quartz/blob/main/docs/tutorials/tutorial-lesson-02.md">Read more.</a>
 * @param <T> Elements type to process
 * @since 1.3
 */
public final class EventsProcessor<T> extends QuartzJob {

    /**
     * Retry attempts amount in the case of error.
     */
    private static final int MAX_RETRY = 3;

    /**
     * Elements.
     */
    private Queue<T> elements;

    /**
     * Action to perform on element.
     */
    private Consumer<T> action;

    @Override
    @SuppressWarnings("PMD.CognitiveComplexity")
    public void execute(final JobExecutionContext context) {
        if (this.action == null || this.elements == null) {
            super.stopJob(context);
        } else {
            int cnt = 0;
            int error = 0;
            while (!this.elements.isEmpty()) {
                final T item = this.elements.poll();
                if (item != null) {
                    try {
                        cnt = cnt + 1;
                        this.action.accept(item);
                    } catch (final EventProcessingError ex) {
                        Logger.error(this, ex.getMessage());
                        if (error > EventsProcessor.MAX_RETRY) {
                            this.stopJob(context);
                            break;
                        }
                        error = error + 1;
                        cnt = cnt - 1;
                        this.elements.add(item);
                    }
                }
            }
            Logger.debug(
                this,
                String.format(
                    "%s: Processed %s elements from queue", Thread.currentThread().getName(), cnt
                )
            );
        }
    }

    /**
     * Set elements queue from job context.
     * @param queue Queue with elements to process
     */
    public void setElements(final Queue<T> queue) {
        this.elements = queue;
    }

    /**
     * Set elements consumer from job context.
     * @param consumer Action to consume the element
     */
    public void setAction(final Consumer<T> consumer) {
        this.action = consumer;
    }

}
