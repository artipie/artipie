/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepoConfigYaml;
import com.artipie.test.TestStoragesCache;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Test for {@link MetadataEventQueues}.
 * @since 0.31
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MetadataEventQueuesTest {

    /**
     * Quartz service.
     */
    private QuartzService service;

    @BeforeEach
    void init() {
        this.service = new QuartzService();
        this.service.start();
    }

    @AfterEach
    void stop() {
        this.service.stop();
    }

    @Test
    void createsQueueAndAddsJob() throws SchedulerException, InterruptedException {
        final RepoConfig cfg = new RepoConfig(
            new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
            new Key.From("my-npm-proxy"),
            new RepoConfigYaml("npm-proxy").withFileStorage(Path.of("a/b/c")).yaml(),
            new TestStoragesCache()
        );
        final MetadataEventQueues events = new MetadataEventQueues(
            new LinkedList<>(), this.service
        );
        final Optional<Queue<ProxyArtifactEvent>> first = events.proxyEventQueues(cfg);
        MatcherAssert.assertThat("Proxy queue should be present", first.isPresent());
        final Optional<Queue<ProxyArtifactEvent>> second = events.proxyEventQueues(cfg);
        MatcherAssert.assertThat(
            "After second call the same queue is returned",
            first.get(), new IsEqual<>(second.get())
        );
        Thread.sleep(2000);
        final List<String> groups = new StdSchedulerFactory().getScheduler().getJobGroupNames();
        MatcherAssert.assertThat(
            "Only one job group exists",
            groups.size(), new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "Only one job exists in this group",
            new StdSchedulerFactory().getScheduler()
                .getJobKeys(GroupMatcher.groupEquals(groups.get(0))).size(),
            new IsEqual<>(1)
        );
    }

    @Test
    void createsQueueAndStartsGivenAmountOfJobs() throws SchedulerException, InterruptedException {
        final RepoConfig cfg = new RepoConfig(
            new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
            new Key.From("my-maven-proxy"),
            new RepoConfigYaml("maven-proxy").withFileStorage(Path.of("a/b/c")).withSettings(
                Yaml.createYamlMappingBuilder().add("threads_count", "4")
                    .add("interval_seconds", "5").build()
            ).yaml(),
            new TestStoragesCache()
        );
        final MetadataEventQueues events =
            new MetadataEventQueues(new LinkedList<>(), this.service);
        final Optional<Queue<ProxyArtifactEvent>> queue = events.proxyEventQueues(cfg);
        MatcherAssert.assertThat("Proxy queue should be present", queue.isPresent());
        Thread.sleep(2000);
        final List<String> groups = new StdSchedulerFactory().getScheduler().getJobGroupNames();
        MatcherAssert.assertThat(
            "Only one job group exists",
            groups.size(), new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "Only one job exists in this group",
            new StdSchedulerFactory().getScheduler()
                .getJobKeys(GroupMatcher.groupEquals(groups.get(0))).size(),
            new IsEqual<>(4)
        );
    }

}
