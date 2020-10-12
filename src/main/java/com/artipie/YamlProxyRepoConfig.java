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
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Proxy repository config from YAML.
 *
 * @since 0.12
 */
public final class YamlProxyRepoConfig implements ProxyRepoConfig {

    /**
     * Source YAML.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     *
     * @param yaml Source YAML.
     */
    public YamlProxyRepoConfig(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    @Override
    public Collection<Remote> remotes() {
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
    private static class YamlRemote implements Remote {

        /**
         * Source YAML.
         */
        private final YamlMapping yaml;

        /**
         * Ctor.
         *
         * @param yaml Source YAML.
         */
        YamlRemote(final YamlMapping yaml) {
            this.yaml = yaml;
        }

        @Override
        public String url() {
            return this.yaml.string("url");
        }

        @Override
        public Authenticator auth() {
            final Authenticator result;
            final String username = this.yaml.string("username");
            final String password = this.yaml.string("password");
            if (username == null && password == null) {
                result = Authenticator.ANONYMOUS;
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
                result = new GenericAuthenticator(username, password);
            }
            return result;
        }
    }
}
