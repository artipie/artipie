/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.events;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Test for {@link NpmProxyPackageProcessor}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NpmProxyPackageProcessorTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    /**
     * Queue with packages and owner names.
     */
    private Queue<ProxyArtifactEvent> packages;

    /**
     * Scheduler.
     */
    private Scheduler scheduler;

    /**
     * Job data map.
     */
    private JobDataMap data;

    @BeforeEach
    void init() throws SchedulerException {
        this.asto = new InMemoryStorage();
        this.events = new LinkedList<>();
        this.packages = new LinkedList<>();
        this.scheduler = new StdSchedulerFactory().getScheduler();
        this.data = new JobDataMap();
        this.data.put("events", this.events);
        this.data.put("packages", this.packages);
        this.data.put("rname", "my-npm");
        this.data.put("storage", this.asto);
        this.data.put("host", "localhost");
    }

    @Test
    void addsEvents() throws SchedulerException {
        this.saveFilesToRegistry();
        this.packages.add(
            new ProxyArtifactEvent(
                new Key.From(
                    "@hello/simple-npm-project", "-", "@hello/simple-npm-project-1.0.1.tgz"
                ),
                "my-npm"
            )
        );
        this.scheduler.scheduleJob(
            JobBuilder.newJob(NpmProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job1", NpmProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger1", NpmProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> this.events.size() == 1);
    }

    @Test
    void doesNotAddsItemToQueueIfTgzNotExists() throws SchedulerException {
        this.packages.add(new ProxyArtifactEvent(new Key.From("not-existing.tgz"), "npm-proxy"));
        this.scheduler.scheduleJob(
            JobBuilder.newJob(NpmProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job1", NpmProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger1", NpmProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        Awaitility.await().pollDelay(8, TimeUnit.SECONDS).until(() -> this.events.size() == 0);
    }

    private void saveFilesToRegistry() {
        new TestResource("storage/@hello/simple-npm-project/meta.json").saveTo(
            this.asto,
            new Key.From("@hello/simple-npm-project", "meta.json")
        );
        new TestResource(
            "storage/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"
        ).saveTo(
            this.asto,
            new Key.From("@hello/simple-npm-project", "-", "@hello/simple-npm-project-1.0.1.tgz")
        );
    }

}
