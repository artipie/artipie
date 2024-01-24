/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.client.Settings;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.npm.RandomFreePort;
import com.artipie.npm.events.NpmProxyPackageProcessor;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Integration test for NPM Proxy.
 *
 * It uses MockServer container to emulate Remote registry responses,
 * and Node container to run npm install command.
 *
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "deprecation"})
@DisabledOnOs(OS.WINDOWS)
@org.testcontainers.junit.jupiter.Testcontainers
public final class NpmProxyITCase {
    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Port to listen for NPM Proxy adapter.
     */
    private static int listenPort;

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices(
        new Settings.WithFollowRedirects(true)
    );

    /**
     * Node test container.
     */
    @org.testcontainers.junit.jupiter.Container
    private final NodeContainer npmcnter = new NodeContainer()
        .withCommand("tail", "-f", "/dev/null");

    /**
     * Verdaccio test container.
     */
    @org.testcontainers.junit.jupiter.Container
    private final VerdaccioContainer verdaccio = new VerdaccioContainer()
        .withExposedPorts(4873);

    /**
     * Vertx slice instance.
     */
    private VertxSliceServer srv;

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    /**
     * Scheduler.
     */
    private Scheduler scheduler;

    @Test
    public void installSingleModule() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "timezone-enum"
        );
        MatcherAssert.assertThat(
            result.getStdout(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("+ timezone-enum@"),
                    new StringContains("added 1 package")
                )
            )
        );
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> this.events.size() == 1);
    }

    @Test
    public void installsModuleWithDependencies() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "http-errors"
        );
        MatcherAssert.assertThat(
            result.getStdout(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("+ http-errors"),
                    new StringContains("added 6 packages")
                )
            )
        );
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> this.events.size() == 6);
        MatcherAssert.assertThat(
            "Contains http-errors",
            this.events.stream().anyMatch(item -> "http-errors".equals(item.artifactName()))
        );
        MatcherAssert.assertThat(
            "Contains depd",
            this.events.stream().anyMatch(item -> "depd".equals(item.artifactName()))
        );
        MatcherAssert.assertThat(
            "Contains inherits",
            this.events.stream().anyMatch(item -> "inherits".equals(item.artifactName()))
        );
        MatcherAssert.assertThat(
            "Contains setprototypeof",
            this.events.stream().anyMatch(item -> "setprototypeof".equals(item.artifactName()))
        );
        MatcherAssert.assertThat(
            "Contains statuses",
            this.events.stream().anyMatch(item -> "statuses".equals(item.artifactName()))
        );
        MatcherAssert.assertThat(
            "Contains toidentifier",
            this.events.stream().anyMatch(item -> "toidentifier".equals(item.artifactName()))
        );
    }

    @Test
    public void packageNotFound() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "packageNotFound"
        );
        MatcherAssert.assertThat(result.getExitCode(), new IsEqual<>(1));
        MatcherAssert.assertThat(
            result.getStderr(),
            new StringContains(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "Not Found - GET http://host.testcontainers.internal:%d/npm-proxy/packageNotFound",
                    NpmProxyITCase.listenPort
                )
            )
        );
        Awaitility.await().pollDelay(8, TimeUnit.SECONDS).until(() -> this.events.size() == 0);
    }

    @Test
    public void assetNotFound() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "assetNotFound"
        );
        MatcherAssert.assertThat(result.getExitCode(), new IsEqual<>(1));
        MatcherAssert.assertThat(
            result.getStderr(),
            new StringContains(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "Not Found - GET http://host.testcontainers.internal:%d/npm-proxy/assetNotFound",
                    NpmProxyITCase.listenPort
                )
            )
        );
        Awaitility.await().pollDelay(8, TimeUnit.SECONDS).until(() -> this.events.size() == 0);
    }

    @BeforeEach
    void setUp() throws Exception {
        final String address = this.verdaccio.getContainerIpAddress();
        final Integer port = this.verdaccio.getFirstMappedPort();
        this.client.start();
        final Storage asto = new InMemoryStorage();
        final URI uri = URI.create(String.format("http://%s:%d", address, port));
        final NpmProxy npm = new NpmProxy(uri, asto, this.client);
        final Queue<ProxyArtifactEvent> packages = new LinkedList<>();
        final NpmProxySlice slice = new NpmProxySlice("npm-proxy", npm, Optional.of(packages));
        this.srv = new VertxSliceServer(NpmProxyITCase.VERTX, slice, NpmProxyITCase.listenPort);
        this.srv.start();
        this.scheduler = new StdSchedulerFactory().getScheduler();
        this.events = new LinkedList<>();
        final JobDataMap data = new JobDataMap();
        data.put("events", this.events);
        data.put("packages", packages);
        data.put("rname", "npm-proxy");
        data.put("storage", asto);
        data.put("host", uri.getPath());
        this.scheduler.scheduleJob(
            JobBuilder.newJob(NpmProxyPackageProcessor.class).setJobData(data).withIdentity(
                "job1", NpmProxyPackageProcessor.class.getSimpleName()
            ).build(),
            TriggerBuilder.newTrigger().startNow()
                .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(5))
                .withIdentity("trigger1", NpmProxyPackageProcessor.class.getSimpleName()).build()
        );
        this.scheduler.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.srv.stop();
        this.client.stop();
        this.scheduler.shutdown();
    }

    @BeforeAll
    static void prepare() throws IOException {
        NpmProxyITCase.listenPort = new RandomFreePort().value();
        Testcontainers.exposeHostPorts(NpmProxyITCase.listenPort);
    }

    @AfterAll
    static void finish() {
        NpmProxyITCase.VERTX.close();
    }

    /**
     * Inner subclass to instantiate Node container.
     * @since 0.1
     */
    private static class NodeContainer extends GenericContainer<NodeContainer> {
        NodeContainer() {
            super("node:14-alpine");
        }
    }

    /**
     * Inner subclass to instantiate Npm container.
     *
     * We need this class because a situation with generics in testcontainers.
     * See https://github.com/testcontainers/testcontainers-java/issues/238
     * @since 0.1
     */
    private static class VerdaccioContainer extends GenericContainer<VerdaccioContainer> {
        VerdaccioContainer() {
            super("verdaccio/verdaccio");
        }
    }
}
