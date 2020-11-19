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

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.MeasuredStorage;
import com.artipie.SliceFromConfig;
import com.artipie.StorageAliases;
import com.artipie.YamlStorage;
import com.artipie.asto.Key;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Proxy repository config from YAML.
 *
 * @since 0.12
 */
public final class YamlProxyConfig implements ProxyConfig {

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
     * @param storages Storages.
     * @param prefix Cache storage prefix.
     * @param yaml Source YAML.
     */
    public YamlProxyConfig(
        final StorageAliases storages,
        final Key prefix,
        final YamlMapping yaml
    ) {
        this.storages = storages;
        this.prefix = prefix;
        this.yaml = yaml;
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
                result = new GenericAuthenticator(SliceFromConfig.HTTP);
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
                result = new GenericAuthenticator(SliceFromConfig.HTTP, username, password);
            }
            return result;
        }

        @Override
        public Optional<Storage> cache() {
            return Optional.ofNullable(this.source.yamlMapping("cache")).flatMap(
                root -> Optional.ofNullable(root.value("storage")).map(
                    node -> {
                        final Storage storage;
                        if (node instanceof Scalar) {
                            storage = YamlProxyConfig.this.storages.storage(
                                ((Scalar) node).value()
                            );
                        } else if (node instanceof YamlMapping) {
                            storage = new YamlStorage((YamlMapping) node).storage();
                        } else {
                            throw new IllegalStateException(
                                String.format("Invalid storage config: %s", node)
                            );
                        }
                        return new MeasuredStorage(
                            new SubStorage(
                                YamlProxyConfig.this.prefix,
                                new LoggingStorage(Level.INFO, storage)
                            )
                        );
                    }
                )
            );
        }
    }
}
