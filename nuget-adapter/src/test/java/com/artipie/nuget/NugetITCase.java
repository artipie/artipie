/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.nuget.http.TestAuthentication;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

/**
 * Integration test for NuGet repository.
 * This test uses docker linux image with nuget client.
 * Authorisation is not used here as NuGet client hangs up on pushing a package
 * when authentication is required.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NugetITCase {

    /**
     * HTTP server hosting NuGet repository.
     */
    private VertxSliceServer server;

    /**
     * Packages source name in config.
     */
    private String source;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Events queue.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() throws Exception {
        this.events = new ConcurrentLinkedQueue<>();
        final int port = new RandomFreePort().get();
        final String base = String.format("http://host.testcontainers.internal:%s", port);
        this.server = new VertxSliceServer(
            new LoggingSlice(
                new NuGet(
                    URI.create(base).toURL(),
                    new AstoRepository(new InMemoryStorage()),
                    Policy.FREE,
                    new TestAuthentication(),
                    "test", Optional.ofNullable(this.events)
                )
            ),
            port
        );
        this.server.start();
        this.cntn = new GenericContainer<>("centeredge/nuget:5")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/");
        Testcontainers.exposeHostPorts(port);
        this.cntn.start();
        this.source = "artipie-nuget-test";
        this.cntn.copyFileToContainer(
            Transferable.of(
                this.configXml(
                    String.format("%s/index.json", base),
                    TestAuthentication.USERNAME,
                    TestAuthentication.PASSWORD
                )
            ),
            "/home/NuGet.Config"
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
    }

    @Test
    void shouldPushPackage() throws Exception {
        MatcherAssert.assertThat(
            this.pushPackage(),
            new StringContains(false, "Your package was pushed.")
        );
        MatcherAssert.assertThat("Events queue has one event", this.events.size() == 1);
    }

    @Test
    void shouldInstallPushedPackage() throws Exception {
        this.pushPackage();
        MatcherAssert.assertThat(
            runNuGet(
                "nuget", "install", "Newtonsoft.Json", "-Version", "12.0.3", "-NoCache",
                "-ConfigFile", "/home/NuGet.Config",
                "-Verbosity", "detailed", "-Source", this.source
            ),
            Matchers.containsString("Successfully installed 'Newtonsoft.Json 12.0.3'")
        );
    }

    private String pushPackage() throws Exception {
        final String file = UUID.randomUUID().toString();
        this.cntn.copyFileToContainer(
            Transferable.of(
                new TestResource("newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg").asBytes()
            ),
            String.format("/home/%s", file)
        );
        return runNuGet(
            "nuget", "push", String.format("/home/%s", file),
            "-ConfigFile", "/home/NuGet.Config",
            "-Verbosity", "detailed", "-Source", this.source
        );
    }

    private byte[] configXml(final String url, final String user, final String pwd) {
        return String.join(
            "",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
            "<configuration>",
            "<packageSources>",
            String.format("<add key=\"%s\" value=\"%s\" />", this.source, url),
            "</packageSources>",
            "<packageSourceCredentials>",
            String.format("<%s>", this.source),
            String.format("<add key=\"Username\" value=\"%s\"/>", user),
            String.format("<add key=\"ClearTextPassword\" value=\"%s\"/>", pwd),
            String.format("</%s>", this.source),
            "</packageSourceCredentials>",
            "</configuration>"
        ).getBytes();
    }

    private String runNuGet(final String... args) throws IOException, InterruptedException {
        final String log = this.cntn.execInContainer(args).toString();
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        return log;
    }

}
