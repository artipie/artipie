/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.CredsConfigYaml;
import com.artipie.settings.users.PasswordFormat;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromYaml}.
 * @since 0.3
 */
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods",
    "PMD.UseObjectForClearerAPI"})
final class AuthFromYamlTest {

    @Test
    void authorisesByPlainPassword() {
        final String user = "john";
        final String pass = "qwerty";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(user, String.format("plain:%s", pass))
            ).user(user, pass).get(),
            new IsEqual<>(new Authentication.User(user))
        );
    }

    @Test
    void authorisesByHashedPassword() {
        final String user = "bob";
        final String pass = "123";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(
                    user, String.format("sha256:%s", DigestUtils.sha256Hex(pass))
                )
            ).user(user, pass).get(),
            new IsEqual<>(new Authentication.User(user))
        );
    }

    @Test
    void doesNotAuthoriseByWrongPassword() {
        final String user = "mark";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(user, "plain:123")
            ).user(user, "456").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseByWrongLogin() {
        final String pass = "abc";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(
                    "ann", String.format("sha256:%s", DigestUtils.sha256Hex(pass))
                )
            ).user("anna", pass).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseWhenPassIsMalformed() {
        final String pass = "098";
        final String user = "barton";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(user, "098")
            ).user(user, pass).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    @Disabled
    void authorisesByAlternativeLogin() {
        final String user = "mary";
        final String pass = "def";
        final String login = "mary@example.com";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(
                    user, String.format("sha256:%s", DigestUtils.sha256Hex(pass)), login
                )
            ).user(login, pass).get(),
            new IsEqual<>(user)
        );
    }

    @Test
    @Disabled
    void doesNotAuthoriseByMainLogin() {
        final String user = "sasha";
        final String pass = "999";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.settings(
                    user,
                    String.format("sha256:%s", DigestUtils.sha256Hex(pass)),
                    "sasha@example.com"
                )
            ).user(user, pass).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void authorisesByPlainPasswordWithSimplerSettings() {
        final String user = "john";
        final String pass = "qwerty";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                new CredsConfigYaml().withUserAndPswd(user, PasswordFormat.PLAIN, pass).yaml()
            ).user(user, pass).get(),
            new IsEqual<>(new Authentication.User(user))
        );
    }

    @Test
    void authorisesByHashedPasswordWithSimplerSettings() {
        final String user = "bob";
        final String pass = "123";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                new CredsConfigYaml()
                    .withUserAndPswd(user, PasswordFormat.SHA256, pass).yaml()
            ).user(user, pass).get(),
            new IsEqual<>(new Authentication.User(user))
        );
    }

    @Test
    void doesNotAuthoriseByWrongPasswordWithSimplerSettings() {
        final String user = "mark";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                new CredsConfigYaml()
                    .withUserAndPswd(user, PasswordFormat.PLAIN, "123").yaml()
            ).user(user, "456").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseByWrongLoginWithSimplerSettings() {
        final String pass = "abc";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                new CredsConfigYaml()
                    .withUserAndPswd("ann", PasswordFormat.SHA256, pass).yaml()
            ).user("anna", pass).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void readsGroups() {
        final List<String> groups = new ListOf<>("readers", "a-team", "c-team");
        final String mark = "mark";
        MatcherAssert.assertThat(
            new AuthFromYaml(new CredsConfigYaml().withUserAndGroups(mark, groups).yaml())
                .user(mark, "123").get(),
            new IsEqual<>(new Authentication.User(mark, groups))
        );
    }

    @Test
    @Disabled
    void authorisesByAlternativeLoginWithSimplerSettings() {
        final String user = "mary";
        final String pass = "def";
        final String login = "mary@example.com";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.simpleSettings(
                    user,
                    "sha256",
                    DigestUtils.sha256Hex(pass),
                    login
                )
            ).user(login, pass).get(),
            new IsEqual<>(user)
        );
    }

    @Test
    @Disabled
    void doesNotAuthoriseByMainLoginWithSimplerSettings() {
        final String user = "sasha";
        final String pass = "999";
        MatcherAssert.assertThat(
            new AuthFromYaml(
                AuthFromYamlTest.simpleSettings(
                    user,
                    "sha256",
                    DigestUtils.sha256Hex(pass),
                    "sasha@example.com"
                )
            ).user(user, pass).isEmpty(),
            new IsEqual<>(true)
        );
    }

    /**
     * Composes yaml settings.
     * @param user User
     * @param type Password type
     * @param pass Password
     * @param login Alternative login
     * @return Settings
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    private static YamlMapping simpleSettings(final String user, final String type,
        final String pass, final String login) {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(
                    user,
                    Yaml.createYamlMappingBuilder()
                    .add("type", type)
                    .add("pass", pass)
                    .add("login", login).build()
                ).build()
        ).build();
    }

    /**
     * Composes yaml settings.
     * @param user User
     * @param pass Password
     * @return Settings
     */
    private static YamlMapping settings(final String user, final String pass) {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(user, Yaml.createYamlMappingBuilder().add("pass", pass).build())
                .build()
        ).build();
    }

    /**
     * Composes yaml settings.
     * @param user User
     * @param pass Password
     * @param login Alternative login
     * @return Settings
     */
    private static YamlMapping settings(final String user, final String pass, final String login) {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(
                    user,
                    Yaml.createYamlMappingBuilder().add("pass", pass).add("login", login).build()
                ).build()
        ).build();
    }
}
