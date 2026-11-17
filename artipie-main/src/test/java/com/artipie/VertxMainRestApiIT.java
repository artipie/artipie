/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.artipie.auth.JwtSecret;
import com.artipie.test.vertxmain.TestVertxMain;
import com.artipie.test.vertxmain.TestVertxMainBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link VertxMain} Rest API JWT secret and Rest API switch (CLS-94600).
 * Starts real Artipie servers in-process.
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class VertxMainRestApiIT {

    /**
     * Previously hardcoded secret.
     */
    private static final String OLD_SECRET = "some secret";

    /**
     * Configured secret.
     */
    private static final String SECRET = "it-secret-0123456789abcdef0123456789";

    /**
     * Rest API endpoint protected by JWT.
     */
    private static final String LIST = "/api/v1/repository/list";

    /**
     * Vertx for signing test tokens.
     */
    private static Vertx vertx;

    /**
     * Http client.
     */
    private HttpClient http;

    /**
     * Started servers.
     */
    private final java.util.List<TestVertxMain> servers = new java.util.ArrayList<>(2);

    @TempDir
    Path temp;

    @BeforeAll
    static void startVertx() {
        VertxMainRestApiIT.vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() {
        VertxMainRestApiIT.vertx.close();
    }

    @BeforeEach
    void setUp() {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void tearDown() {
        this.servers.forEach(TestVertxMain::close);
    }

    @Test
    void rejectsTokenForgedWithOldHardcodedSecret() throws Exception {
        final TestVertxMain server = this.start("srv", Collections.emptyMap());
        MatcherAssert.assertThat(
            this.get(server, VertxMainRestApiIT.forged("admin")).statusCode(),
            new IsEqual<>(401)
        );
        MatcherAssert.assertThat(
            "Forging an existing admin user must not help",
            this.get(server, VertxMainRestApiIT.forged("jwt-admin")).statusCode(),
            new IsEqual<>(401)
        );
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        final TestVertxMain server = this.start("srv", Collections.emptyMap());
        MatcherAssert.assertThat(this.get(server, null).statusCode(), new IsEqual<>(401));
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        final TestVertxMain server = this.start("srv", Collections.emptyMap());
        MatcherAssert.assertThat(
            this.get(server, "not.a.jwt").statusCode(), new IsEqual<>(401)
        );
    }

    @Test
    void acceptsTokenIssuedByRestApiWithRandomSecret() throws Exception {
        final TestVertxMain server = this.start("srv", Collections.emptyMap());
        MatcherAssert.assertThat(
            this.get(server, this.issue(server, "jwt-admin", "jwt-admin-pass")).statusCode(),
            new IsEqual<>(200)
        );
    }

    @Test
    void refusesToIssueTokenForWrongPassword() throws Exception {
        final TestVertxMain server = this.start("srv", Collections.emptyMap());
        MatcherAssert.assertThat(
            this.post(server, "jwt-admin", "wrong").statusCode(), new IsEqual<>(401)
        );
    }

    @Test
    void acceptsTokenSignedWithConfiguredSecret() throws Exception {
        final TestVertxMain server = this.start(
            "srv", Map.of(JwtSecret.ENV, VertxMainRestApiIT.SECRET)
        );
        MatcherAssert.assertThat(
            this.get(
                server, VertxMainRestApiIT.sign(VertxMainRestApiIT.SECRET, "jwt-admin")
            ).statusCode(),
            new IsEqual<>(200)
        );
        MatcherAssert.assertThat(
            this.get(server, this.issue(server, "jwt-admin", "jwt-admin-pass")).statusCode(),
            new IsEqual<>(200)
        );
        MatcherAssert.assertThat(
            this.get(server, VertxMainRestApiIT.forged("jwt-admin")).statusCode(),
            new IsEqual<>(401)
        );
    }

    @Test
    void sharesConfiguredSecretBetweenInstances() throws Exception {
        final Map<String, String> env = Map.of(JwtSecret.ENV, VertxMainRestApiIT.SECRET);
        final TestVertxMain first = this.start("first", env);
        final TestVertxMain second = this.start("second", env);
        MatcherAssert.assertThat(
            this.get(second, this.issue(first, "jwt-admin", "jwt-admin-pass")).statusCode(),
            new IsEqual<>(200)
        );
    }

    @Test
    void doesNotShareRandomSecretBetweenInstances() throws Exception {
        final TestVertxMain first = this.start("first", Collections.emptyMap());
        final TestVertxMain second = this.start("second", Collections.emptyMap());
        MatcherAssert.assertThat(
            this.get(second, this.issue(first, "jwt-admin", "jwt-admin-pass")).statusCode(),
            new IsEqual<>(401)
        );
    }

    @Test
    void failsToStartWithShortConfiguredSecret() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> new TestVertxMainBuilder(this.temp.resolve("short"))
                .withUser("jwt-admin", "security/users/jwt-admin.yaml")
                .build(
                    TestVertxMainBuilder.freePort(), TestVertxMainBuilder.freePort(),
                    new JwtSecret(Map.of(JwtSecret.ENV, VertxMainRestApiIT.OLD_SECRET))
                )
        );
    }

    @Test
    void doesNotStartRestApiWhenPortIsZero() throws Exception {
        Assumptions.assumeTrue(
            !VertxMainRestApiIT.listening(8086), "Default Rest API port 8086 is busy"
        );
        final TestVertxMain server = this.startWithApiPort("off", 0);
        MatcherAssert.assertThat(
            "Repository port must keep serving",
            this.http.send(
                HttpRequest.newBuilder(
                    URI.create(String.format("http://localhost:%d/.health", server.port()))
                ).build(),
                HttpResponse.BodyHandlers.ofString()
            ).statusCode(),
            new IsEqual<>(200)
        );
        Thread.sleep(1000);
        MatcherAssert.assertThat(
            "Rest API must not fall back to default port 8086",
            VertxMainRestApiIT.listening(8086), new IsEqual<>(false)
        );
    }

    @Test
    void doesNotStartRestApiWhenPortIsNegative() throws Exception {
        final int unused = TestVertxMainBuilder.freePort();
        final TestVertxMain server = this.startWithApiPort("neg", -1);
        MatcherAssert.assertThat(
            this.http.send(
                HttpRequest.newBuilder(
                    URI.create(String.format("http://localhost:%d/.health", server.port()))
                ).build(),
                HttpResponse.BodyHandlers.ofString()
            ).statusCode(),
            new IsEqual<>(200)
        );
        Thread.sleep(1000);
        MatcherAssert.assertThat(VertxMainRestApiIT.listening(unused), new IsEqual<>(false));
    }

    @Test
    void startsRestApiOnPositivePort() throws Exception {
        final TestVertxMain server = this.start("on", Collections.emptyMap());
        MatcherAssert.assertThat(VertxMainRestApiIT.listening(server.apiPort()), new IsEqual<>(true));
    }

    /**
     * Start server with Rest API on a free port and wait until Rest API is up.
     * @param name Work dir name
     * @param env Environment for the JWT secret
     * @return Server
     * @throws Exception On error
     */
    private TestVertxMain start(final String name, final Map<String, String> env)
        throws Exception {
        final TestVertxMain server = new TestVertxMainBuilder(this.temp.resolve(name))
            .withUser("jwt-admin", "security/users/jwt-admin.yaml")
            .build(TestVertxMainBuilder.freePort(), TestVertxMainBuilder.freePort(), new JwtSecret(env));
        this.servers.add(server);
        final long deadline = System.currentTimeMillis() + 30_000;
        while (!VertxMainRestApiIT.listening(server.apiPort())) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Rest API did not start");
            }
            Thread.sleep(100);
        }
        return server;
    }

    /**
     * Start server with the given Rest API port.
     * @param name Work dir name
     * @param api Rest API port
     * @return Server
     * @throws IOException On error
     */
    private TestVertxMain startWithApiPort(final String name, final int api) throws IOException {
        final TestVertxMain server = new TestVertxMainBuilder(this.temp.resolve(name))
            .withUser("jwt-admin", "security/users/jwt-admin.yaml")
            .build(TestVertxMainBuilder.freePort(), api, new JwtSecret(Collections.emptyMap()));
        this.servers.add(server);
        return server;
    }

    /**
     * GET protected Rest API endpoint.
     * @param server Server
     * @param token Bearer token or null
     * @return Response
     * @throws Exception On error
     */
    private HttpResponse<String> get(final TestVertxMain server, final String token)
        throws Exception {
        final HttpRequest.Builder req = HttpRequest.newBuilder(
            URI.create(
                String.format("http://localhost:%d%s", server.apiPort(), VertxMainRestApiIT.LIST)
            )
        );
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return this.http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * POST token request.
     * @param server Server
     * @param name User
     * @param pass Password
     * @return Response
     * @throws Exception On error
     */
    private HttpResponse<String> post(final TestVertxMain server, final String name,
        final String pass) throws Exception {
        return this.http.send(
            HttpRequest.newBuilder(
                URI.create(
                    String.format("http://localhost:%d/api/v1/oauth/token", server.apiPort())
                )
            ).header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new JsonObject().put("name", name).put("pass", pass).encode()
                    )
                ).build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    /**
     * Obtain token from Rest API.
     * @param server Server
     * @param name User
     * @param pass Password
     * @return Token
     * @throws Exception On error
     */
    private String issue(final TestVertxMain server, final String name, final String pass)
        throws Exception {
        final HttpResponse<String> res = this.post(server, name, pass);
        MatcherAssert.assertThat(res.body(), res.statusCode(), new IsEqual<>(200));
        return new JsonObject(res.body()).getString("token");
    }

    /**
     * Token forged with the old hardcoded secret.
     * @param sub Subject
     * @return Token
     */
    private static String forged(final String sub) {
        return VertxMainRestApiIT.sign(VertxMainRestApiIT.OLD_SECRET, sub);
    }

    /**
     * Sign token.
     * @param secret Secret
     * @param sub Subject
     * @return Token
     */
    private static String sign(final String secret, final String sub) {
        return JWTAuth.create(
            VertxMainRestApiIT.vertx,
            new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(secret)
            )
        ).generateToken(new JsonObject().put("sub", sub).put("context", "artipie"));
    }

    /**
     * Whether something listens on localhost port.
     * @param port Port
     * @return True if listening
     */
    private static boolean listening(final int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 500);
            return true;
        } catch (final ConnectException ex) {
            return false;
        } catch (final IOException ex) {
            return false;
        }
    }
}
