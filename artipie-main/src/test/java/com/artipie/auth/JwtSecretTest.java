/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.AuthUser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link JwtSecret}.
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class JwtSecretTest {

    /**
     * Previously hardcoded secret (CLS-94600).
     */
    private static final String OLD_SECRET = "some secret";

    /**
     * Valid configured secret, exactly 32 bytes.
     */
    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

    /**
     * Vertx instance.
     */
    private static Vertx vertx;

    @BeforeAll
    static void startVertx() {
        JwtSecretTest.vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() {
        JwtSecretTest.vertx.close();
    }

    @Test
    void returnsConfiguredSecret() {
        MatcherAssert.assertThat(
            new JwtSecret(Map.of(JwtSecret.ENV, JwtSecretTest.SECRET_32)).value(),
            new IsEqual<>(JwtSecretTest.SECRET_32)
        );
    }

    @Test
    void returnsSameConfiguredSecretOnEveryCall() {
        final JwtSecret secret = new JwtSecret(Map.of(JwtSecret.ENV, JwtSecretTest.SECRET_32));
        MatcherAssert.assertThat(secret.value(), new IsEqual<>(secret.value()));
    }

    @Test
    void acceptsLongConfiguredSecret() {
        final String secret = "x".repeat(128);
        MatcherAssert.assertThat(
            new JwtSecret(Map.of(JwtSecret.ENV, secret)).value(),
            new IsEqual<>(secret)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"x1", "some secret", "0123456789abcdef0123456789abcde"})
    void rejectsShortConfiguredSecret(final String value) {
        final IllegalStateException err = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new JwtSecret(Map.of(JwtSecret.ENV, value)).value()
        );
        MatcherAssert.assertThat(err.getMessage(), new StringContains(JwtSecret.ENV));
        MatcherAssert.assertThat(
            "Error message must not leak the configured secret",
            err.getMessage(), new IsNot<>(new StringContains(value))
        );
    }

    @Test
    void countsSecretLengthInUtf8Bytes() {
        // 11 three-byte euro signs = 33 bytes, though only 11 characters long
        final String secret = "€".repeat(11);
        MatcherAssert.assertThat(
            new JwtSecret(Map.of(JwtSecret.ENV, secret)).value(),
            new IsEqual<>(secret)
        );
    }

    @Test
    void generatesRandomSecretWhenNotConfigured() {
        final String secret = new JwtSecret(Collections.emptyMap()).value();
        MatcherAssert.assertThat(
            Base64.getDecoder().decode(secret).length,
            new IsEqual<>(JwtSecret.MIN_BYTES)
        );
        MatcherAssert.assertThat(secret, new IsNot<>(new IsEqual<>(JwtSecretTest.OLD_SECRET)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void generatesRandomSecretWhenBlank(final String value) {
        MatcherAssert.assertThat(
            Base64.getDecoder().decode(new JwtSecret(Map.of(JwtSecret.ENV, value)).value()).length,
            new IsEqual<>(JwtSecret.MIN_BYTES)
        );
    }

    @Test
    void generatesDifferentSecretsEachTime() {
        final JwtSecret secret = new JwtSecret(Collections.emptyMap());
        final Set<String> values = new HashSet<>();
        for (int idx = 0; idx < 100; idx += 1) {
            values.add(secret.value());
        }
        MatcherAssert.assertThat(values.size(), new IsEqual<>(100));
    }

    @Test
    void ignoresOtherEnvironmentVariables() {
        MatcherAssert.assertThat(
            new JwtSecret(Map.of("JWT_SECRET", JwtSecretTest.SECRET_32)).value(),
            new IsNot<>(new IsEqual<>(JwtSecretTest.SECRET_32))
        );
    }

    @Test
    void tokenSignedWithOldHardcodedSecretIsRejected() {
        final JWTAuth forger = JwtSecretTest.provider(JwtSecretTest.OLD_SECRET);
        final String forged = forger.generateToken(
            new JsonObject().put("sub", "admin").put("context", "artipie")
        );
        for (final JwtSecret secret : new JwtSecret[] {
            new JwtSecret(Collections.emptyMap()),
            new JwtSecret(Map.of(JwtSecret.ENV, JwtSecretTest.SECRET_32)),
        }) {
            MatcherAssert.assertThat(
                new JwtTokenAuth(JwtSecretTest.provider(secret.value())).user(forged)
                    .toCompletableFuture().join().isEmpty(),
                new IsEqual<>(true)
            );
        }
    }

    @Test
    void tokenIssuedWithConfiguredSecretIsAccepted() {
        final JWTAuth provider = JwtSecretTest.provider(
            new JwtSecret(Map.of(JwtSecret.ENV, JwtSecretTest.SECRET_32)).value()
        );
        final AuthUser user = new AuthUser("alice", "artipie");
        MatcherAssert.assertThat(
            new JwtTokenAuth(provider).user(new JwtTokens(provider).generate(user))
                .toCompletableFuture().join().get(),
            new IsEqual<>(user)
        );
    }

    @Test
    void tokenIssuedWithRandomSecretIsAcceptedBySameProviderOnly() {
        final JWTAuth first = JwtSecretTest.provider(new JwtSecret(Collections.emptyMap()).value());
        final JWTAuth second = JwtSecretTest.provider(
            new JwtSecret(Collections.emptyMap()).value()
        );
        final AuthUser user = new AuthUser("alice", "artipie");
        final String token = new JwtTokens(first).generate(user);
        MatcherAssert.assertThat(
            new JwtTokenAuth(first).user(token).toCompletableFuture().join().get(),
            new IsEqual<>(user)
        );
        MatcherAssert.assertThat(
            new JwtTokenAuth(second).user(token).toCompletableFuture().join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    /**
     * HS256 provider with the given secret.
     * @param secret Secret
     * @return JWT provider
     */
    private static JWTAuth provider(final String secret) {
        return JWTAuth.create(
            JwtSecretTest.vertx,
            new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(secret)
            )
        );
    }
}
