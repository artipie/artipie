/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Test for {@link MavenProxyPackageProcessorTest}.
 * @since 0.10
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MavenProxyPackageProcessorTest {

    /**
     * Repository name.
     */
    private static final String RNAME = "my-maven-proxy";

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
        this.events = new ConcurrentLinkedDeque<>();
        this.packages = new ConcurrentLinkedDeque<>();
        this.scheduler = new StdSchedulerFactory().getScheduler();
        this.data = new JobDataMap();
        this.data.put("events", this.events);
        this.data.put("packages", this.packages);
        this.data.put("storage", this.asto);
    }

    @Test
    void processesPackage() throws SchedulerException {
        final String pkg = "com/artipie/asto/0.15";
        final Key key = new Key.From(pkg);
        new TestResource(pkg).addFilesTo(this.asto, key);
        this.packages.add(new ProxyArtifactEvent(key, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(new ProxyArtifactEvent(key, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(new ProxyArtifactEvent(key, MavenProxyPackageProcessorTest.RNAME));
        this.scheduler.scheduleJob(
            JobBuilder.newJob(MavenProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job1", MavenProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger1", MavenProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> this.events.size() == 1);
        MatcherAssert.assertThat(
            "Same items were removed from packages queue", this.packages.isEmpty()
        );
        final ArtifactEvent event = this.events.poll();
        Assertions.assertEquals("com.artipie.asto", event.artifactName());
        Assertions.assertEquals("0.15", event.artifactVersion());
    }

    @Disabled("https://github.com/artipie/artipie/issues/1349")
    @Test
    void processesSeveralPackagesAndPackageWithError() throws SchedulerException {
        final String first = "com/artipie/asto/0.20.1";
        final Key firstk = new Key.From(first);
        new TestResource(first).addFilesTo(this.asto, firstk);
        final String second = "com/artipie/helloworld/0.1";
        final Key secondk = new Key.From(second);
        new TestResource(second).addFilesTo(this.asto, secondk);
        final String snapshot = "com/artipie/asto/1.0-SNAPSHOT";
        final Key snapshotk = new Key.From(snapshot);
        new TestResource(snapshot).addFilesTo(this.asto, snapshotk);
        this.scheduler.scheduleJob(
            JobBuilder.newJob(MavenProxyPackageProcessor.class).setJobData(this.data).withIdentity(
                "job2", MavenProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withIdentity("trigger2", MavenProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
        this.packages.add(new ProxyArtifactEvent(firstk, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(new ProxyArtifactEvent(snapshotk, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(new ProxyArtifactEvent(secondk, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(
            new ProxyArtifactEvent(new Key.From("fake"), MavenProxyPackageProcessorTest.RNAME)
        );
        this.packages.add(new ProxyArtifactEvent(snapshotk, MavenProxyPackageProcessorTest.RNAME));
        this.packages.add(new ProxyArtifactEvent(firstk, MavenProxyPackageProcessorTest.RNAME));
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> this.events.size() == 3);
        Assertions.assertTrue(
            this.packages.isEmpty(),
            "Same items were removed from packages queue"
        );
    }

}
