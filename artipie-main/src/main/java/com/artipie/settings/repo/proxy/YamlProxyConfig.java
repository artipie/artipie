/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.settings.repo.RepoConfig;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Proxy repository config from YAML.
 *
 * @since 0.12
 * @checkstyle MemberNameCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 */
public final class YamlProxyConfig implements ProxyConfig {

    /**
     * Repository config.
     */
    private final RepoConfig repoConfig;

    /**
     * Source YAML.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     *
     * @param repoConfig Repository config.
     * @param yaml Source YAML.
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public YamlProxyConfig(
        final RepoConfig repoConfig,
        final YamlMapping yaml
    ) {
        this.repoConfig = repoConfig;
        this.yaml = yaml;
    }

    /**
     * Ctor.
     *
     * @param repoConfig Repo configuration.
     */
    public YamlProxyConfig(final RepoConfig repoConfig) {
        this(repoConfig, repoConfig.repoYaml());
    }

    @Override
    public Collection<YamlRemote> remotes() {
        return StreamSupport.stream(
            Optional.ofNullable(
                this.yaml.yamlSequence("remotes")
            ).orElseGet(
                () -> Yaml.createYamlSequenceBuilder().build()
            ).spliterator(),
            false
        ).map(
            remote -> {
                if (!(remote instanceof YamlMapping)) {
                    throw new IllegalStateException(
                        "`remotes` element is not mapping in proxy config"
                    );
                }
                return new YamlRemote((YamlMapping) remote);
            }
        ).collect(Collectors.toList());
    }

    @Override
    public Optional<CacheStorage> cache() {
        return this.repoConfig.storageOpt().map(YamlProxyStorage::new);
    }

    /**
     * Proxy repository remote from YAML.
     *
     * @since 0.12
     */
    public final class YamlRemote implements Remote {

        /**
         * Source YAML.
         */
        private final YamlMapping source;

        /**
         * Ctor.
         *
         * @param source Source YAML.
         */
        YamlRemote(final YamlMapping source) {
            this.source = source;
        }

        @Override
        public String url() {
            return Optional.ofNullable(this.source.string("url")).orElseThrow(
                () -> new IllegalStateException("`url` is not specified for proxy remote")
            );
        }

        @Override
        public Authenticator auth(final ClientSlices client) {
            final Authenticator result;
            final String username = this.source.string("username");
            final String password = this.source.string("password");
            if (username == null && password == null) {
                result = new GenericAuthenticator(client);
            } else {
                if (username == null) {
                    throw new IllegalStateException(
                        "`username` is not specified for proxy remote"
                    );
                }
                if (password == null) {
                    throw new IllegalStateException(
                        "`password` is not specified for proxy remote"
                    );
                }
                result = new GenericAuthenticator(client, username, password);
            }
            return result;
        }
    }
}
