/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.http.HelmSlice;
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.auth.Authentication;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.io.ByteStreams;
import io.vertx.reactivex.core.Vertx;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Ensure that helm command line tool is compatible with this adapter.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.2
 */
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class HelmSliceIT {
    /**
     * Vert instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Chart name.
     */
    private static final String CHART = "tomcat-0.4.1.tgz";

    /**
     * Username.
     */
    private static final String USER = "alice";

    /**
     * User password.
     */
    private static final String PSWD = "123";

    /**
     * The helm container.
     */
    private HelmContainer cntn;

    /**
     * Test container url.
     */
    private String url;

    /**
     * The server.
     */
    private VertxSliceServer server;

    /**
     * Port.
     */
    private int port;

    /**
     * URL connection.
     */
    private HttpURLConnection con;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.events = new ConcurrentLinkedQueue<>();
    }

    @AfterAll
    static void tearDownAll() {
        HelmSliceIT.VERTX.close();
    }

    @AfterEach
    void tearDown() {
        this.con.disconnect();
        this.cntn.stop();
        this.server.close();
    }

    @Test
    void indexYamlIsCreated() throws Exception {
        this.init(true);
        this.con = this.putToLocalhost(true);
        MatcherAssert.assertThat(
            "Response status is not 200",
            this.con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        MatcherAssert.assertThat(
            "Generated index does not contain required chart",
            new ContentOfIndex(this.storage).index()
                .byChartAndVersion("tomcat", "0.4.1")
                .isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("One item was added into events queue", this.events.size() == 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void helmRepoAddAndUpdateWorks(final boolean anonymous) throws Exception {
        final String hostport = this.init(anonymous);
        this.con = this.putToLocalhost(anonymous);
        MatcherAssert.assertThat(
            "Response status is 200",
            this.con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        exec(
            "helm", "init",
            "--stable-repo-url",
            String.format(
                "http://%s:%s@%s",
                HelmSliceIT.USER, HelmSliceIT.PSWD,
                hostport
            ),
            "--client-only", "--debug"
        );
        MatcherAssert.assertThat(
            "Chart repository was added",
            this.helmRepoAdd(anonymous, "chartrepo"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "helm repo update is successful",
            exec("helm", "repo", "update"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "One item was added into events queue", this.events.size() == 1
        );
    }

    private String init(final boolean anonymous) {
        this.port = new RandomFreePort().get();
        final String hostport = String.format("host.testcontainers.internal:%d/", this.port);
        this.url = String.format("http://%s", hostport);
        Testcontainers.exposeHostPorts(this.port);
        if (anonymous) {
            this.server = new VertxSliceServer(
                HelmSliceIT.VERTX,
                new LoggingSlice(
                    new HelmSlice(
                        this.storage,
                        this.url,
                        Policy.FREE,
                        (usr, pwd) -> Optional.of(Authentication.ANONYMOUS),
                        "*",
                        Optional.of(this.events)
                    )
                ),
                this.port
            );
        } else {
            this.server = new VertxSliceServer(
                HelmSliceIT.VERTX,
                new LoggingSlice(
                    new HelmSlice(
                        this.storage,
                        this.url,
                        new PolicyByUsername(HelmSliceIT.USER),
                        new Authentication.Single(HelmSliceIT.USER, HelmSliceIT.PSWD),
                        "test", Optional.of(this.events)
                    )
                ),
                this.port
            );
        }
        this.cntn = new HelmContainer()
            .withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("/bin/sh").withCmd("-c", "while sleep 3600; do :; done")
            );
        this.server.start();
        this.cntn.start();
        return hostport;
    }

    private boolean helmRepoAdd(final boolean anonymous, final String chartrepo) throws Exception {
        final List<String> cmdlst = new ArrayList<>(
            Arrays.asList("helm", "repo", "add", chartrepo, this.url)
        );
        if (!anonymous) {
            cmdlst.add("--username");
            cmdlst.add(HelmSliceIT.USER);
            cmdlst.add("--password");
            cmdlst.add(HelmSliceIT.PSWD);
        }
        final String[] cmdarr = cmdlst.toArray(new String[0]);
        return this.exec(cmdarr);
    }

    private HttpURLConnection putToLocalhost(final boolean anonymous) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) URI.create(
            String.format("http://localhost:%d/%s", this.port, HelmSliceIT.CHART)
        ).toURL().openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        if (!anonymous) {
            Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            HelmSliceIT.USER, HelmSliceIT.PSWD.toCharArray()
                        );
                    }
                }
            );
        }
        ByteStreams.copy(
            new ByteArrayInputStream(
                new TestResource(HelmSliceIT.CHART).asBytes()
            ),
            conn.getOutputStream()
        );
        return conn;
    }

    private boolean exec(final String... cmd) throws IOException, InterruptedException {
        final String joined = String.join(" ", cmd);
        LoggerFactory.getLogger(HelmSliceIT.class).info("Executing:\n{}", joined);
        final Container.ExecResult exec = this.cntn.execInContainer(cmd);
        LoggerFactory.getLogger(HelmSliceIT.class)
            .info("STDOUT:\n{}\nSTDERR:\n{}", exec.getStdout(), exec.getStderr());
        final int code = exec.getExitCode();
        if (code != 0) {
            LoggerFactory.getLogger(HelmSliceIT.class)
                .error("'{}' failed with {} code", joined, code);
        }
        return code == 0;
    }

    /**
     * Inner subclass to instantiate Helm container.
     *
     * @since 0.2
     */
    private static class HelmContainer extends
        GenericContainer<HelmContainer> {
        HelmContainer() {
            super("alpine/helm:2.12.1");
        }
    }
}
