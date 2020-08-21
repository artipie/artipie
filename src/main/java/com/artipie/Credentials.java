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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.api.ContentAs;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Artipie credentials.
 * @since 0.9
 */
public interface Credentials {

    /**
     * Artipie users list.
     * @return Yaml as completion action
     */
    CompletionStage<List<String>> users();

    /**
     * Adds user to artipie users.
     * @param username User name
     * @param pswd Password
     * @return Completion add action
     */
    CompletionStage<Void> add(String username, String pswd);

    /**
     * Removes user from artipie users.
     * @param username User to delete
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String username);

    /**
     * Credentials from main artipie config.
     * @since 0.9
     */
    final class FromStorageYaml implements Credentials {

        /**
         * Credentials yaml mapping key.
         */
        private static final String CREDENTIALS = "credentials";

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Credentials key.
         */
        private final Key key;

        /**
         * Ctor.
         * @param storage Storage
         * @param key Credentials key
         */
        public FromStorageYaml(final Storage storage, final Key key) {
            this.storage = storage;
            this.key = key;
        }

        @Override
        public CompletionStage<List<String>> users() {
            return this.yaml().thenApply(
                yaml -> yaml.yamlMapping(FromStorageYaml.CREDENTIALS).keys()
                    .stream().map(node -> node.asScalar().value()).collect(Collectors.toList())
            );
        }

        @Override
        public CompletionStage<Void> add(final String username, final String pswd) {
            return this.yaml().thenCompose(
                yaml -> {
                    YamlMappingBuilder result = FromStorageYaml.removeUserRecord(username, yaml);
                    result = result.add(
                        username,
                        Yaml.createYamlMappingBuilder()
                            .add("pass", String.format("plain:%s", pswd))
                            .build()
                    );
                    return this.buildAndSaveCredentials(result);
                }
            );
        }

        @Override
        public CompletionStage<Void> remove(final String username) {
            return this.yaml().thenCompose(
                yaml -> this.buildAndSaveCredentials(
                    FromStorageYaml.removeUserRecord(username, yaml)
                )
            );
        }

        /**
         * Credentials as yaml.
         * @return Completion action with yaml
         */
        public CompletionStage<YamlMapping> yaml() {
            return new RxStorageWrapper(this.storage)
                .value(this.key)
                .to(ContentAs.YAML)
                .to(SingleInterop.get())
                .thenApply(yaml -> (YamlMapping) yaml);
        }

        /**
         * Build credentials yaml from users yaml mapping and saves it to storage.
         * @param users Users mapping
         * @return Credentials yaml string representation
         */
        private CompletionStage<Void> buildAndSaveCredentials(final YamlMappingBuilder users) {
            return this.storage.save(
                this.key,
                new Content.From(
                    Yaml.createYamlMappingBuilder()
                        .add(FromStorageYaml.CREDENTIALS, users.build()).build()
                        .toString().getBytes(StandardCharsets.UTF_8)
                )
            );
        }

        /**
         * Removes user record from credentials.yaml.
         * @param username User name to remove
         * @param yaml Credentials mapping
         * @return YamlMappingBuilder without removed user
         */
        private static YamlMappingBuilder removeUserRecord(final String username,
            final YamlMapping yaml) {
            YamlMappingBuilder result = Yaml.createYamlMappingBuilder();
            final YamlMapping credentials = yaml.yamlMapping(FromStorageYaml.CREDENTIALS);
            final List<YamlNode> keep = credentials.keys().stream()
                .filter(node -> !node.asScalar().value().equals(username))
                .collect(Collectors.toList());
            for (final YamlNode node : keep) {
                result = result.add(node, credentials.value(node));
            }
            return result;
        }
    }
}
