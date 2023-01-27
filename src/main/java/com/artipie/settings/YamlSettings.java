/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.users.Users;
import com.artipie.settings.users.UsersFromEnv;
import com.artipie.settings.users.UsersFromGithub;
import com.artipie.settings.users.UsersFromKeycloak;
import com.artipie.settings.users.UsersFromStorageYaml;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class YamlSettings implements Settings {

    /**
     * YAML node name `credentials` for credentials yaml section.
     */
    public static final String NODE_CREDENTIALS = "credentials";

    /**
     * YAML node name `type` for credentials type.
     */
    private static final String NODE_TYPE = "type";

    /**
     * YAML node name for `path` credentials type.
     */
    private static final String NODE_PATH = "path";

    /**
     * YAML node name for `ssl` yaml section.
     */
    private static final String NODE_SSL = "ssl";

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * A set of caches for settings.
     */
    private final ArtipieCaches caches;

    /**
     * Metrics context.
     */
    private final MetricsContext mctx;

    /**
     * Ctor.
     * @param content YAML file content.
     * @param caches Settings caches
     */
    public YamlSettings(final YamlMapping content, final ArtipieCaches caches) {
        this.content = content;
        this.caches = caches;
        this.mctx = new MetricsContext(this.meta());
    }

    @Override
    public Storage configStorage() {
        return this.caches.storagesCache().storage(this);
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return this.credentials().thenCompose(
            Users::auth
        ).thenCompose(this::withAlternative);
    }

    @Override
    public Layout layout() {
        return Layout.valueOf(this.meta().string("layout"));
    }

    @Override
    public YamlMapping meta() {
        return Optional.ofNullable(this.content.yamlMapping("meta"))
            .orElseThrow(
                () -> new IllegalStateException(
                    "Invalid settings: not empty `meta` section is expected"
                )
            );
    }

    @Override
    public Storage repoConfigsStorage() {
        return Optional.ofNullable(this.meta().string("repo_configs"))
            .<Storage>map(str -> new SubStorage(new Key.From(str), this.configStorage()))
            .orElse(this.configStorage());
    }

    @Override
    public CompletionStage<Users> credentials() {
        return this.credentialsYamlSequence().map(
            s -> s.values().stream()
                .map(YamlNode::asMapping)
                .map(this::users)
                .findFirst()
                .orElseThrow()
        ).orElse(
            this.users(this.meta().yamlMapping(YamlSettings.NODE_CREDENTIALS))
        );
    }

    @Override
    public Optional<Key> credentialsKey() {
        return this.credentialsYamlSequence().map(
            seq -> seq.values().stream()
                .filter(
                    node -> CredentialsType.FILE.toString()
                        .equalsIgnoreCase(node.asMapping().string(YamlSettings.NODE_TYPE))
                ).findFirst().map(YamlNode::asMapping)
        ).orElse(Optional.ofNullable(this.meta().yamlMapping(YamlSettings.NODE_CREDENTIALS)))
            .map(file -> new Key.From(file.string(YamlSettings.NODE_PATH)));
    }

    @Override
    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(this.meta().yamlMapping(YamlSettings.NODE_SSL))
            .map(KeyStoreFactory::newInstance);
    }

    @Override
    public MetricsContext metrics() {
        return this.mctx;
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

    /**
     * Credentials YAML sequence.
     *
     * @return YAML sequence.
     */
    private Optional<YamlSequence> credentialsYamlSequence() {
        return Optional.ofNullable(
            this.meta().yamlSequence(YamlSettings.NODE_CREDENTIALS)
        );
    }

    /**
     * Users from yaml file.
     *
     * @param cred YAML credentials.
     * @return Completion action with {@code Users}.
     */
    private CompletionStage<Users> users(final YamlMapping cred) {
        return CredentialsType
            .valueOf(cred)
            .users(this, cred);
    }

    /**
     * Full chain of authentication.
     *
     * @param auth Authentication from credentials.
     * @return Completion action with {@code Authentication}.
     */
    private CompletionStage<Authentication> withAlternative(
        final Authentication auth
    ) {
        CompletionStage<Authentication> res = CompletableFuture
            .completedStage(auth);
        final Optional<YamlSequence> seq = this.credentialsYamlSequence();
        if (seq.isPresent()) {
            final List<CompletionStage<Users>> list = seq.get().values()
                .stream()
                .skip(1)
                .map(YamlNode::asMapping)
                .map(YamlSettings.this::users)
                .toList();
            for (final CompletionStage<Users> users : list) {
                res = res.thenCompose(
                    prev -> users
                        .thenCompose(Users::auth)
                        .thenApply(next -> new Authentication.Joined(prev, next))
                );
            }
        }
        return res;
    }

    /**
     * Credentials types.
     *
     * @since 0.22
     */
    enum CredentialsType {

        /**
         * Credentials type: file.
         */
        FILE((settings, mapping) -> {
            CompletionStage<Users> res = CompletableFuture.failedFuture(
                new RuntimeException(
                    "Invalid credentials configuration: type `file` requires `path`!"
                )
            );
            final String path = mapping.string(YamlSettings.NODE_PATH);
            if (path != null) {
                final Storage storage = settings.configStorage();
                final KeyFromPath key = new KeyFromPath(path);
                res = storage.exists(key).thenApply(
                    exists -> {
                        final Users users;
                        if (exists) {
                            users = new UsersFromStorageYaml(
                                storage,
                                key,
                                settings.caches.credsConfig()
                            );
                        } else {
                            users = new UsersFromEnv();
                        }
                        return users;
                    }
                );
            }
            return res;
        }),

        /**
         * Credentials type: github.
         */
        GITHUB((settings, mapping) -> CompletableFuture.completedStage(
            new UsersFromGithub()
        )),

        /**
         * Credentials type: env.
         */
        ENV((settings, mapping) -> CompletableFuture.completedStage(
            new UsersFromEnv()
        )),

        /**
         * Credentials type: keycloak.
         */
        KEYCLOAK((settings, mapping) -> CompletableFuture.completedStage(
            new UsersFromKeycloak(mapping)
        ));

        /**
         * Transform yaml to completion action with users.
         */
        private final BiFunction<YamlSettings, YamlMapping, CompletionStage<Users>> map;

        /**
         * Ctor.
         *
         * @param map Transform yaml to completion action with users.
         */
        CredentialsType(final BiFunction<YamlSettings, YamlMapping, CompletionStage<Users>> map) {
            this.map = map;
        }

        /**
         * Get {@code CredentialsType} that corresponds to type from credentials
         * yaml mapping or {@code ENV} if {@code yaml} is null.
         *
         * @param yaml Credentials yaml mapping.
         * @return CredentialsType.
         */
        static CredentialsType valueOf(final YamlMapping yaml) {
            CredentialsType res = ENV;
            if (yaml != null) {
                res = CredentialsType.valueOf(
                    yaml.string(YamlSettings.NODE_TYPE).toUpperCase(Locale.getDefault())
                );
            }
            return res;
        }

        /**
         * Transform yaml to completion action with users.
         *
         * @param settings YamlSettings.
         * @param yaml Credentials yaml mapping.
         * @return Completion action with users.
         */
        CompletionStage<Users> users(
            final YamlSettings settings,
            final YamlMapping yaml
        ) {
            return this.map.apply(settings, yaml);
        }
    }
}
