/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.artipie.auth.JwtSecret;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.io.IOException;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for Rest API JWT secret and Rest API switch in the Artipie docker image
 * (CLS-94600).
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class RestApiSecurityITCase {

    /**
     * Configured secret for the env-configured container.
     */
    private static final String SECRET = "itcase-secret-0123456789abcdef012345";

    /**
     * Artipie start command with Rest API disabled.
     */
    private static final String NO_API_CMD = String.join(
        " ",
        "java $JVM_ARGS --add-opens java.base/java.util=ALL-UNNAMED",
        "--add-opens java.base/java.security=ALL-UNNAMED",
        "-cp /usr/lib/artipie/artipie.jar:/usr/lib/artipie/lib/* com.artipie.VertxMain",
        "--config-file=/etc/artipie/artipie.yml --port=8080 --api-port=0"
    );

    /**
     * Vertx for signing tokens.
     */
    private static Vertx vertx;

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        Map.ofEntries(
            new MapEntry<>(
                "artipie",
                () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
                    .withUser("security/users/jwt-admin.yaml", "jwt-admin")
            ),
            new MapEntry<>(
                "artipie-env",
                () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
                    .withUser("security/users/jwt-admin.yaml", "jwt-admin")
                    .withEnv(JwtSecret.ENV, RestApiSecurityITCase.SECRET)
            ),
            new MapEntry<>(
                "artipie-noapi",
                () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
                    .withUser("security/users/jwt-admin.yaml", "jwt-admin")
                    .withCommand("sh", "-c", RestApiSecurityITCase.NO_API_CMD)
            )
        ),
        () -> new TestDeployment.ClientContainer("alpine:3.19").withWorkingDirectory("/w")
    );

    @BeforeAll
    static void startVertx() {
        RestApiSecurityITCase.vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() {
        RestApiSecurityITCase.vertx.close();
    }

    @BeforeEach
    void setUp() throws IOException {
        this.deployment.assertExec(
            "Failed to install deps", new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
        this.waitFor("http://artipie:8086/api/v1/repository/list");
        this.waitFor("http://artipie-env:8086/api/v1/repository/list");
        this.waitFor("http://artipie-noapi:8080/.health");
    }

    @Test
    void rejectsTokenForgedWithOldHardcodedSecret() throws IOException {
        for (final String host : new String[] {"artipie", "artipie-env"}) {
            this.assertStatus(
                host, RestApiSecurityITCase.sign("some secret", "jwt-admin"), "401"
            );
        }
    }

    @Test
    void acceptsTokenIssuedByRestApi() throws IOException {
        for (final String host : new String[] {"artipie", "artipie-env"}) {
            this.assertStatus(host, this.issue(host), "200");
        }
    }

    @Test
    void acceptsTokenSignedWithConfiguredSecret() throws IOException {
        this.assertStatus(
            "artipie-env", RestApiSecurityITCase.sign(RestApiSecurityITCase.SECRET, "jwt-admin"),
            "200"
        );
    }

    @Test
    void doesNotShareRandomSecretWithOtherInstance() throws IOException {
        this.assertStatus("artipie", this.issue("artipie-env"), "401");
    }

    @Test
    void restApiPortIsClosedWhenDisabled() throws IOException {
        this.deployment.assertExec(
            "Rest API must not listen when --api-port=0",
            new ContainerResultMatcher(new IsEqual<>(7)),
            "curl", "-s", "-o", "/dev/null", "--max-time", "5",
            "http://artipie-noapi:8086/api/v1/oauth/token"
        );
    }

    @Test
    void repositoryPortServesWhenRestApiDisabled() throws IOException {
        this.deployment.assertExec(
            "Repository port must keep serving",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("200")
            ),
            "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
            "http://artipie-noapi:8080/.health"
        );
    }

    /**
     * Assert Rest API status for a token.
     * @param host Artipie host
     * @param token Token
     * @param status Expected HTTP status
     * @throws IOException On error
     */
    private void assertStatus(final String host, final String token, final String status)
        throws IOException {
        this.deployment.assertExec(
            String.format("Unexpected Rest API status on %s", host),
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains(status)
            ),
            "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
            "-H", String.format("Authorization: Bearer %s", token),
            String.format("http://%s:8086/api/v1/repository/list", host)
        );
    }

    /**
     * Obtain token from Rest API.
     * @param host Artipie host
     * @return Token
     * @throws IOException On error
     */
    private String issue(final String host) throws IOException {
        final String out = this.deployment.exec(
            "curl", "-s", "-X", "POST", "-H", "Content-Type: application/json",
            "-d", "{\"name\":\"jwt-admin\",\"pass\":\"jwt-admin-pass\"}",
            String.format("http://%s:8086/api/v1/oauth/token", host)
        ).getStdout();
        return new JsonObject(out).getString("token");
    }

    /**
     * Wait until URL responds.
     * @param url Url
     * @throws IOException On error
     */
    private void waitFor(final String url) throws IOException {
        this.deployment.assertExec(
            String.format("%s did not come up", url), new ContainerResultMatcher(),
            "curl", "-s", "-o", "/dev/null", "--retry", "30", "--retry-delay", "1",
            "--retry-all-errors", url
        );
    }

    /**
     * Sign token.
     * @param secret Secret
     * @param sub Subject
     * @return Token
     */
    private static String sign(final String secret, final String sub) {
        return JWTAuth.create(
            RestApiSecurityITCase.vertx,
            new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(secret)
            )
        ).generateToken(new JsonObject().put("sub", sub).put("context", "artipie"));
    }
}
