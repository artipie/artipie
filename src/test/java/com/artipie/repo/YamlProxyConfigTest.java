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
package com.artipie.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.StorageAliases;
import com.artipie.asto.Key;
import com.artipie.http.client.auth.GenericAuthenticator;
import java.util.Collection;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link YamlProxyConfig}.
 *
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class YamlProxyConfigTest {

    @Test
    public void parsesConfig() {
        final String firsturl = "https://artipie.com";
        final String secondurl = "http://localhost:8080/path";
        final Collection<YamlProxyConfig.YamlRemote> remotes = this.remotes(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", firsturl)
                        .add("username", "alice")
                        .add("password", "qwerty")
                        .build()
                ).add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", secondurl)
                        .add(
                            "cache",
                            Yaml.createYamlMappingBuilder().add(
                                "storage",
                                Yaml.createYamlMappingBuilder()
                                    .add("type", "fs")
                                    .add("path", "/var/artipie/data")
                                    .build()
                            ).build()
                        )
                        .build()
                ).build()
            ).build()
        );
        MatcherAssert.assertThat(
            "Both remotes parsed",
            remotes.size(),
            new IsEqual<>(2)
        );
        final ProxyConfig.Remote first = remotes.stream().findFirst().get();
        MatcherAssert.assertThat(
            "First remote URL parsed",
            first.url(),
            new IsEqual<>(firsturl)
        );
        MatcherAssert.assertThat(
            "First remote authenticator is GenericAuthenticator",
            first.auth(),
            new IsInstanceOf(GenericAuthenticator.class)
        );
        MatcherAssert.assertThat(
            "Second remote is absent",
            first.cache().isPresent(),
            new IsEqual<>(false)
        );
        final ProxyConfig.Remote second = remotes.stream().skip(1).findFirst().get();
        MatcherAssert.assertThat(
            "Second remote URL parsed",
            second.url(),
            new IsEqual<>(secondurl)
        );
        MatcherAssert.assertThat(
            "Second remote authenticator is GenericAuthenticator",
            second.auth(),
            new IsInstanceOf(GenericAuthenticator.class)
        );
        MatcherAssert.assertThat(
            "Second remote has cache",
            second.cache().isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    public void parsesEmpty() {
        final Collection<? extends ProxyConfig.Remote> remotes = this.remotes(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().build()
            ).build()
        );
        MatcherAssert.assertThat(
            remotes,
            new IsEmptyCollection<>()
        );
    }

    @Test
    public void failsToGetUrlWhenNotSpecified() {
        final ProxyConfig.Remote remote = this.remotes(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder().add("attr", "value").build()
                ).build()
            ).build()
        ).iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class,
            remote::url
        );
    }

    @Test
    public void failsToGetAuthWhenUsernameOnly() {
        final ProxyConfig.Remote remote = this.remotes(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", "https://artipie.com")
                        .add("username", "bob")
                        .build()
                ).build()
            ).build()
        ).iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class,
            remote::auth
        );
    }

    @Test
    public void failsToGetAuthWhenPasswordOnly() {
        final ProxyConfig.Remote remote = this.remotes(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", "https://artipie.com")
                        .add("password", "12345")
                        .build()
                ).build()
            ).build()
        ).iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class,
            remote::auth
        );
    }

    @Test
    public void returnsEmptyCollectionWhenYamlEmpty() {
        final Collection<YamlProxyConfig.YamlRemote> remote =
            this.remotes(Yaml.createYamlMappingBuilder().build());
        MatcherAssert.assertThat(
            remote.isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    public void throwsExceptionWhenYamlRemotesIsNotMapping() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () ->
                this.remotes(Yaml.createYamlMappingBuilder().add(
                    "remotes",
                    Yaml.createYamlSequenceBuilder().add(
                        Yaml.createYamlSequenceBuilder()
                            .add("url:http://localhost:8080")
                            .add("username:alice")
                            .add("password:qwerty")
                            .build()
                    ).build()
                ).build()
            )
        );
    }

    private Collection<YamlProxyConfig.YamlRemote> remotes(final YamlMapping yaml) {
        return new YamlProxyConfig(
            StorageAliases.EMPTY, Key.ROOT, yaml
        ).remotes();
    }

}
