/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link YamlAuth}.
 * @since 0.3
 */
@Disabled
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlAuthTest {

    @Test
    void authorisesByPlainPassword() {
        final String user = "john";
        final String pass = "qwerty";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(user, String.format("plain:%s", pass))
            ).user(user, Base64.getEncoder().encodeToString(pass.getBytes())).get(),
            new IsEqual<>(user)
        );
    }

    @Test
    void authorisesByHashedPassword() {
        final String user = "bob";
        final String pass = "123";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(user, String.format("sha256:%s", DigestUtils.sha256Hex(pass)))
            ).user(user, Base64.getEncoder().encodeToString(pass.getBytes())).get(),
            new IsEqual<>(user)
        );
    }

    @Test
    void authorisesByAlternativeLogin() {
        final String user = "mary";
        final String pass = "def";
        final String login = "mary@example.com";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(
                    user, String.format("sha256:%s", DigestUtils.sha256Hex(pass)), login
                )
            ).user(login, Base64.getEncoder().encodeToString(pass.getBytes())).get(),
            new IsEqual<>(user)
        );
    }

    @Test
    void doesNotAuthoriseByMainLogin() {
        final String user = "sasha";
        final String pass = "999";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(
                    user,
                    String.format("sha256:%s", DigestUtils.sha256Hex(pass)),
                    "sasha@example.com"
                )
            ).user(user, Base64.getEncoder().encodeToString(pass.getBytes())).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseByWrongPassword() {
        final String user = "mark";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(user, "plain:123")
            ).user(user, Base64.getEncoder().encodeToString("456".getBytes())).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthoriseByWrongLogin() {
        final String pass = "abc";
        MatcherAssert.assertThat(
            new YamlAuth(
                YamlAuthTest.settings(
                    "ann", String.format("sha256:%s", DigestUtils.sha256Hex(pass))
                )
            ).user("anna", Base64.getEncoder().encodeToString(pass.getBytes())).isEmpty(),
            new IsEqual<>(true)
        );
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
