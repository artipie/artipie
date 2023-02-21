/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link AuthFromStorage}.
 * @since 1.29
 * @checkstyle MethodNameCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class AuthFromStorageTest {

    /**
     * Test storage.
     */
    private BlockingStorage asto;

    @BeforeEach
    void init() {
        this.asto = new BlockingStorage(new InMemoryStorage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"users/alice.yaml", "users/alice.yml"})
    void authorisesUserWithPlainPassword(final String key) {
        this.asto.save(new Key.From(key), this.aliceConfig());
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("alice", "qwerty").get(),
            new IsEqual<>(new Authentication.User("alice"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"users/alice.yaml", "users/alice.yml"})
    void notAuthorisesUserWithPlainPasswordIfPasswordNotCorrect(final String key) {
        this.asto.save(new Key.From(key), this.aliceConfig());
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("alice", "not_correct").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"users/david.yaml", "users/david.yml"})
    void authorisesUserWithSha256Password(final String key) {
        this.asto.save(new Key.From(key), this.davidConfig());
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("david", "abc123").get(),
            new IsEqual<>(new Authentication.User("david"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"users/david.yaml", "users/david.yml"})
    void notAuthorisesUserWithSha256PasswordIfPasswordNotCorrect(final String key) {
        this.asto.save(new Key.From(key), this.davidConfig());
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("david", "not_valid").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseDisabledUser() {
        this.asto.save(new Key.From("users/jane.yml"), this.janeConfig());
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("jane", "qwerty").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseIfUserNotExists() {
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("notPresent", "any").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseIfUserYamlIsNotValid() {
        this.asto.save(new Key.From("users/olga.yml"), "any text".getBytes(StandardCharsets.UTF_8));
        MatcherAssert.assertThat(
            new AuthFromStorage(this.asto).user("olga", "any").isEmpty(),
            new IsEqual<>(true)
        );
    }

    private byte[] aliceConfig() {
        return String.join(
            "\n",
            "alice:",
            "  type: plain",
            "  pass: qwerty",
            "  email: alice@example.com"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] davidConfig() {
        return String.join(
            "\n",
            "david:",
            "  type: sha256",
            String.format("  pass: %s", DigestUtils.sha256Hex("abc123")),
            "  enabled: true"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] janeConfig() {
        return String.join(
            "\n",
            "jane:",
            "  type: plain",
            "  pass: qwerty",
            "  enabled: false"
        ).getBytes(StandardCharsets.UTF_8);
    }

}
