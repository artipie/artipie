/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.test.TestStoragesCache;
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
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class YamlProxyConfigTest {

    @Test
    public void parsesConfig() {
        final String firsturl = "https://artipie.com";
        final String secondurl = "http://localhost:8080/path";
        final YamlProxyConfig config = this.proxyConfig(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", firsturl)
                        .add("username", "alice")
                        .add("password", "qwerty").build()
                ).add(Yaml.createYamlMappingBuilder().add("url", secondurl).build()).build()
            ).add(
                "storage",
                Yaml.createYamlMappingBuilder().add("type", "fs").add("path", "a/b/c").build()
            ).build()
        );
        MatcherAssert.assertThat(
            "Storage cache is present",
            config.cache().isPresent(),
            new IsEqual<>(true)
        );
        final Collection<YamlProxyConfig.YamlRemote> remotes = config.remotes();
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
            first.auth(new JettyClientSlices()),
            new IsInstanceOf(GenericAuthenticator.class)
        );
        final ProxyConfig.Remote second = remotes.stream().skip(1).findFirst().get();
        MatcherAssert.assertThat(
            "Second remote URL parsed",
            second.url(),
            new IsEqual<>(secondurl)
        );
        MatcherAssert.assertThat(
            "Second remote authenticator is GenericAuthenticator",
            second.auth(new JettyClientSlices()),
            new IsInstanceOf(GenericAuthenticator.class)
        );
    }

    @Test
    public void parsesEmpty() {
        final Collection<? extends ProxyConfig.Remote> remotes = this.proxyConfig(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().build()
            ).build()
        ).remotes();
        MatcherAssert.assertThat(
            remotes,
            new IsEmptyCollection<>()
        );
    }

    @Test
    public void failsToGetUrlWhenNotSpecified() {
        final ProxyConfig.Remote remote = this.proxyConfig(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder().add("attr", "value").build()
                ).build()
            ).build()
        ).remotes().iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class,
            remote::url
        );
    }

    @Test
    public void failsToGetAuthWhenUsernameOnly() {
        final ProxyConfig.Remote remote = this.proxyConfig(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", "https://artipie.com")
                        .add("username", "bob")
                        .build()
                ).build()
            ).build()
        ).remotes().iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class, () -> remote.auth(new JettyClientSlices())
        );
    }

    @Test
    public void failsToGetAuthWhenPasswordOnly() {
        final ProxyConfig.Remote remote = this.proxyConfig(
            Yaml.createYamlMappingBuilder().add(
                "remotes",
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("url", "https://artipie.com")
                        .add("password", "12345")
                        .build()
                ).build()
            ).build()
        ).remotes().iterator().next();
        Assertions.assertThrows(
            IllegalStateException.class, () -> remote.auth(new JettyClientSlices())
        );
    }

    @Test
    public void returnsEmptyCollectionWhenYamlEmpty() {
        final Collection<YamlProxyConfig.YamlRemote> remote =
            this.proxyConfig(Yaml.createYamlMappingBuilder().build()).remotes();
        MatcherAssert.assertThat(
            remote.isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    public void throwsExceptionWhenYamlRemotesIsNotMapping() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.proxyConfig(
                Yaml.createYamlMappingBuilder().add(
                    "remotes",
                    Yaml.createYamlSequenceBuilder().add(
                        Yaml.createYamlSequenceBuilder()
                            .add("url:http://localhost:8080")
                            .add("username:alice")
                            .add("password:qwerty")
                            .build()
                    ).build()
                ).build()
            ).remotes()
        );
    }

    private YamlProxyConfig proxyConfig(final YamlMapping yaml) {
        return new YamlProxyConfig(
            new RepoConfig(
                new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
                Key.ROOT,
                Yaml.createYamlMappingBuilder().add("repo", yaml).build(),
                new TestStoragesCache()
            ),
            yaml
        );
    }

}
