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
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromYaml;
import com.artipie.http.auth.Authentication;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;

/**
 * Artipie credentials.
 * @since 0.9
 */
public interface Credentials {

    /**
     * Artipie users list.
     * @return Yaml as completion action
     */
    CompletionStage<List<User>> users();

    /**
     * Adds user to artipie users.
     * @param user User info
     * @param pswd Password
     * @param format Password format
     * @return Completion add action
     */
    CompletionStage<Void> add(User user, String pswd, PasswordFormat format);

    /**
     * Removes user from artipie users.
     * @param username User to delete
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String username);

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     */
    CompletionStage<Authentication> auth();

    /**
     * Password format.
     * @since 0.11
     */
    enum PasswordFormat {

        /**
         * Plain password format.
         */
        PLAIN,

        /**
         * Sha256 password format.
         */
        SHA256
    }

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
         * Credentials yaml mapping key.
         */
        private static final String EMAIL = "email";

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
        public CompletionStage<List<User>> users() {
            return this.yaml().thenApply(
                yaml -> yaml.yamlMapping(FromStorageYaml.CREDENTIALS).keys().stream()
                    .map(
                        node -> {
                            final String name = node.asScalar().value();
                            return new User(
                                name,
                                Optional.ofNullable(
                                    yaml.yamlMapping(FromStorageYaml.CREDENTIALS)
                                        .yamlMapping(name).string(FromStorageYaml.EMAIL)
                                )
                            );
                        }
                    ).collect(Collectors.toList())
            );
        }

        @Override
        public CompletionStage<Void> add(final User user, final String pswd,
            final PasswordFormat format) {
            return this.yaml().thenCompose(
                yaml -> {
                    YamlMappingBuilder result = FromStorageYaml.removeUserRecord(user.uname, yaml);
                    YamlMappingBuilder info = Yaml.createYamlMappingBuilder()
                        .add(
                            "pass",
                            String.format("%s:%s", format.name().toLowerCase(Locale.US), pswd)
                        );
                    if (user.mail.isPresent()) {
                        info = info.add(FromStorageYaml.EMAIL, user.mail.get());
                    }
                    result = result.add(user.uname, info.build());
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

        @Override
        public CompletionStage<Authentication> auth() {
            return this.yaml().thenApply(AuthFromYaml::new);
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

    /**
     * Credentials from env.
     * @since 0.10
     */
    final class FromEnv implements Credentials {

        /**
         * Environment variables.
         */
        private final Map<String, String> env;

        /**
         * Ctor.
         */
        public FromEnv() {
            this(System.getenv());
        }

        /**
         * Ctor.
         * @param env Environment variables
         */
        public FromEnv(final Map<String, String> env) {
            this.env = env;
        }

        @Override
        public CompletionStage<List<User>> users() {
            return CompletableFuture.completedFuture(
                Optional.ofNullable(this.env.get(AuthFromEnv.ENV_NAME))
                    .<List<User>>map(name -> new ListOf<>(new User(name, Optional.empty())))
                    .orElse(Collections.emptyList())
            );
        }

        @Override
        public CompletionStage<Void> add(final User user, final String pswd,
            final PasswordFormat format) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Adding users is not supported")
            );
        }

        @Override
        public CompletionStage<Void> remove(final String username) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Removing users is not supported")
            );
        }

        @Override
        public CompletionStage<Authentication> auth() {
            return CompletableFuture.completedFuture(new AuthFromEnv());
        }
    }

    /**
     * User.
     * @since 0.11
     */
    final class User {

        /**
         * User name.
         */
        private final String uname;

        /**
         * User email.
         */
        private final Optional<String> mail;

        /**
         * Ctor.
         * @param name Username
         * @param email User email
         */
        public User(final String name, final Optional<String> email) {
            this.uname = name;
            this.mail = email;
        }

        /**
         * Get user name.
         * @return Name of the user
         */
        public String name() {
            return this.uname;
        }

        /**
         * Get user email.
         * @return Email of the user
         */
        public Optional<String> email() {
            return this.mail;
        }
    }
}
