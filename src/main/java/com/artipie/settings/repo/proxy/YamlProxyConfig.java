/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.StorageYamlConfig;
import com.artipie.settings.repo.RepoConfig;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Proxy repository config from YAML.
 *
 * @since 0.12
 */
public final class YamlProxyConfig implements ProxyConfig {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Storages.
     */
    private final StorageAliases storages;

    /**
     * Cache storage prefix.
     */
    private final Key prefix;

    /**
     * Source YAML.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     *
     * @param http HTTP client
     * @param storages Storages.
     * @param prefix Cache storage prefix.
     * @param yaml Source YAML.
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public YamlProxyConfig(
        final ClientSlices http,
        final StorageAliases storages,
        final Key prefix,
        final YamlMapping yaml
    ) {
        this.http = http;
        this.storages = storages;
        this.prefix = prefix;
        this.yaml = yaml;
    }

    /**
     * Ctor.
     *
     * @param http HTTP client
     * @param repo Repo configuration.
     */
    public YamlProxyConfig(final ClientSlices http, final RepoConfig repo) {
        this(http, repo.storageAliases(), new Key.From(repo.name()), repo.repoConfig());
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
        public Authenticator auth() {
            final Authenticator result;
            final String username = this.source.string("username");
            final String password = this.source.string("password");
            if (username == null && password == null) {
                result = new GenericAuthenticator(YamlProxyConfig.this.http);
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
                result = new GenericAuthenticator(YamlProxyConfig.this.http, username, password);
            }
            return result;
        }

        @Override
        public Optional<CacheStorage> cache() {
            return Optional.ofNullable(this.source.yamlMapping("cache")).flatMap(
                root -> Optional.ofNullable(root.value("storage")).map(
                    node -> new YamlProxyStorage(
                        new StorageYamlConfig(
                            node, YamlProxyConfig.this.storages
                        ).subStorage(YamlProxyConfig.this.prefix)
                    )
                )
            );
        }
    }
}
