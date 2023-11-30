/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * IT case for {@link GoSlice}: it runs Testcontainer with latest version of golang,
 * starts up Vertx server with {@link GoSlice} and sets up go module `time` using go adapter.
 * @since 0.3
 * @todo #62:30min Make this test work with authorization, for now go refuses to send username and
 *  password parameters to insecure url with corresponding error: "refusing to pass credentials
 *  to insecure URL".
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.StaticAccessToStaticFields")
@DisabledOnOs(OS.WINDOWS)
public final class GoSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test module version.
     */
    private static final String VERSION = "v0.0.0-20191024005414-555d28b269f0";

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Alice", "wonderland");

    /**
     * Vertx instance.
     */
    private static VertxSliceServer slice;

    /**
     * GoLang container to verify Go repository layout.
     */
    private static GenericContainer<?> golang;

    @ParameterizedTest
    @ValueSource(booleans = {true})
    void installsTimeModule(final boolean anonymous) throws Exception {
        GoSliceITCase.startContainer(anonymous);
        MatcherAssert.assertThat(
            GoSliceITCase.golang
                .execInContainer("go", "get", "golang.org/x/time").getStderr(),
            new StringContains(String.format("go: golang.org/x/time upgrade => %s", VERSION))
        );
    }

    static void startContainer(final boolean anonymous) throws Exception {
        GoSliceITCase.slice = new VertxSliceServer(
            GoSliceITCase.VERTX,
            new SliceRoute(
                new RtRulePath(
                    new RtRule.ByPath(".*"),
                    new GoSlice(
                        GoSliceITCase.create(), GoSliceITCase.perms(anonymous),
                        GoSliceITCase.users(anonymous), "test"
                    )
                )
            )
        );
        final int port = GoSliceITCase.slice.start();
        Testcontainers.exposeHostPorts(port);
        final String template = "http://%shost.testcontainers.internal:%d";
        final String url;
        if (anonymous) {
            url = String.format(template, "", port);
        } else {
            url = String.format(
                template,
                String.format("%s:%s@", GoSliceITCase.USER.getKey(), GoSliceITCase.USER.getValue()),
                port
            );
        }
        GoSliceITCase.golang = new GenericContainer<>("golang:1.15.12")
            .withEnv("GOPROXY", url)
            .withEnv("GO111MODULE", "on")
            .withEnv("GOSUMDB", "off")
            .withEnv("GOINSECURE", "host.testcontainers.internal*")
            .withCommand("tail", "-f", "/dev/null");
        GoSliceITCase.golang.start();
    }

    @AfterAll
    static void stopContainer() {
        GoSliceITCase.slice.close();
        GoSliceITCase.VERTX.close();
        GoSliceITCase.golang.stop();
    }

    /**
     * Permissions.
     * @param anonymous Is auth required?
     * @return Permissions instance
     */
    private static Policy<?> perms(final boolean anonymous) {
        final Policy<?> res;
        if (anonymous) {
            res = Policy.FREE;
        } else {
            res = new PolicyByUsername(GoSliceITCase.USER.getKey());
        }
        return res;
    }

    /**
     * Identities.
     * @param anonymous Is auth required?
     * @return Identities instance
     */
    private static Authentication users(final boolean anonymous) {
        final Authentication res;
        if (anonymous) {
            res = (usr, pwd) -> Optional.of(Authentication.ANONYMOUS);
        } else {
            res = new Authentication.Single(
                GoSliceITCase.USER.getKey(), GoSliceITCase.USER.getValue()
            );
        }
        return res;
    }

    /**
     * Creates test storage.
     * @return Storage
     * @throws Exception If smth wrong
     */
    private static Storage create() throws Exception {
        final Storage res = new InMemoryStorage();
        final String path = "/golang.org/x/time/@v/%s%s";
        final String zip = ".zip";
        //@checkstyle LineLengthCheck (4 lines)
        res.save(new KeyFromPath(String.format(path, "", "list")), new Content.From(VERSION.getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, ".info")), new Content.From(String.format("{\"Version\":\"%s\",\"Time\":\"2019-10-24T00:54:14Z\"}", VERSION).getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, ".mod")), new Content.From("module golang.org/x/time".getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, zip)), new Content.From(new TestResource(String.format("%s%s", VERSION, zip)).asBytes())).get();
        return res;
    }
}
