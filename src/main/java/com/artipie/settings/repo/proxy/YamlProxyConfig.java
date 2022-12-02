/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.Key;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.settings.repo.RepoConfig;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
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
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Repository config.
     */
    private final RepoConfig repoConfig;

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
     * @param repoConfig Repository config.
     * @param prefix Cache storage prefix.
     * @param yaml Source YAML.
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public YamlProxyConfig(
        final ClientSlices http,
        final RepoConfig repoConfig,
        final Key prefix,
        final YamlMapping yaml
    ) {
        this.http = http;
        this.repoConfig = repoConfig;
        this.prefix = prefix;
        this.yaml = yaml;
    }

    /**
     * Ctor.
     *
     * @param http HTTP client
     * @param repoConfig Repo configuration.
     */
    public YamlProxyConfig(final ClientSlices http, final RepoConfig repoConfig) {
        this(http, repoConfig, new Key.From(repoConfig.name()), repoConfig.repoYaml());
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
     * Creates proxy storage.
     *
     * @param node Storage config or alias.
     * @return Storage.
     */
    Storage storageForProxy(final YamlNode node) {
        return new SubStorage(
            this.prefix,
            new LoggingStorage(
                Level.INFO,
                this.repoConfig.storageAliases()
                    .storage(this.repoConfig.storagesCache(), node)
            )
        );
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
                        YamlProxyConfig.this.storageForProxy(node)
                    )
                )
            );
        }
    }
}
