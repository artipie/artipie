/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Test for {@link PyProxyPackageProcessor}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PyProxyPackageProcessorTest {

    /**
     * Repository name.
     */
    private static final String REPO_NAME = "my-pypi-proxy";

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
        this.data.put("storage", this.asto);
    }

    @Test
    void checkPackagesAndAddsToQueue() throws SchedulerException {
        final Key zip = new Key.From("artipie-sample-0.2.zip");
        final Key tar = new Key.From("artipie-sample-0.2.tar");
        final Key whl = new Key.From("artipie_sample-0.2-py3-none-any.whl");
        new TestResource("pypi_repo/artipie-sample-0.2.zip").saveTo(this.asto, zip);
        new TestResource("pypi_repo/artipie-sample-0.2.tar").saveTo(this.asto, tar);
        new TestResource("pypi_repo/artipie_sample-0.2-py3-none-any.whl").saveTo(this.asto, whl);
        this.packages.add(new ProxyArtifactEvent(zip, PyProxyPackageProcessorTest.REPO_NAME));
        this.packages.add(new ProxyArtifactEvent(tar, PyProxyPackageProcessorTest.REPO_NAME));
        this.packages.add(new ProxyArtifactEvent(whl, PyProxyPackageProcessorTest.REPO_NAME));
        this.scheduler.scheduleJob(
            JobBuilder.newJob(PyProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job1", PyProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger1", PyProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> this.events.size() == 3);
    }

    @Test
    void doNotAddNotValidPackage() throws SchedulerException {
        final Key tar = new Key.From("artipie-sample-0.2.tar");
        final Key invalid = new Key.From("invalid.zip");
        this.asto.save(invalid, Content.EMPTY).join();
        new TestResource("pypi_repo/artipie-sample-0.2.tar").saveTo(this.asto, tar);
        this.packages.add(new ProxyArtifactEvent(invalid, PyProxyPackageProcessorTest.REPO_NAME));
        this.packages.add(new ProxyArtifactEvent(tar, PyProxyPackageProcessorTest.REPO_NAME));
        this.scheduler.scheduleJob(
            JobBuilder.newJob(PyProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job1", PyProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger1", PyProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> this.events.size() == 1);
    }

    @AfterEach
    void stop() throws SchedulerException {
        this.scheduler.shutdown();
    }
}
