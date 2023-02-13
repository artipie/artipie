/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.ArtipieException;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromKeycloak;
import com.artipie.auth.AuthFromYaml;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.CachedCreds;
import com.artipie.settings.cache.CachedStorages;
import com.artipie.settings.cache.CachedUsers;
import com.artipie.settings.cache.StoragesCache;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import org.keycloak.authorization.client.Configuration;

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
     * A set of caches for artipie settings.
     */
    private final ArtipieCaches acach;

    /**
     * Storages cache.
     */
    private final StoragesCache storages;

    /**
     * Metrics context.
     */
    private final MetricsContext mctx;

    /**
     * Authentication.
     */
    private final Authentication authn;

    /**
     * Ctor.
     * @param content YAML file content.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public YamlSettings(final YamlMapping content) {
        this.content = content;
        this.storages = new CachedStorages();
        final CachedUsers users = new CachedUsers(this.initializeAuth());
        this.authn = users;
        this.acach = new ArtipieCaches.All(users, this.storages);
        this.mctx = new MetricsContext(this.meta());
    }

    @Override
    public Storage configStorage() {
        return this.storages.storage(this);
    }

    @Override
    public Authentication auth() {
        return this.authn;
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
    public ArtipieCaches caches() {
        return this.acach;
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

    /**
     * Initialise authentication taking into account various possible authentication types.
     * @return Authentication instance, not cached
     */
    private Authentication initializeAuth() {
        return this.withAlternative(
            this.credentialsYamlSequence().map(
                s -> s.values().stream()
                    .map(YamlNode::asMapping)
                    .map(this::auth)
                    .findFirst()
                    .orElseThrow()
            ).orElse(
                this.auth(this.meta().yamlMapping(YamlSettings.NODE_CREDENTIALS))
            )
        );
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
     * Authentication from yaml file.
     *
     * @param cred YAML credentials.
     * @return Completion action with {@code Users}.
     */
    private Authentication auth(final YamlMapping cred) {
        return CredentialsType.valueOf(cred).auth(this, cred);
    }

    /**
     * Full chain of authentication.
     *
     * @param auth Authentication from credentials.
     * @return Completion action with {@code Authentication}.
     */
    private Authentication withAlternative(final Authentication auth) {
        Authentication res = auth;
        final Optional<YamlSequence> seq = this.credentialsYamlSequence();
        if (seq.isPresent()) {
            final List<Authentication> list = seq.get().values()
                .stream()
                .skip(1)
                .map(YamlNode::asMapping)
                .map(YamlSettings.this::auth)
                .toList();
            for (final Authentication users : list) {
                res = new Authentication.Joined(res, users);
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
            final String path = mapping.string(YamlSettings.NODE_PATH);
            if (path == null) {
                throw new ArtipieException(
                    "Invalid credentials configuration: type `file` requires `path`!"
                );
            } else {
                final Storage storage = settings.configStorage();
                final KeyFromPath key = new KeyFromPath(path);
                return storage.exists(key).thenCompose(
                    exists -> {
                        final CompletionStage<Authentication> auth;
                        if (exists) {
                            auth = new CachedCreds().credentials(storage, key)
                                .thenApply(AuthFromYaml::new);
                        } else {
                            auth = CompletableFuture.completedStage(new AuthFromEnv());
                        }
                        return auth;
                    }
                ).join();
            }
        }),

        /**
         * Credentials type: github.
         */
        GITHUB((settings, mapping) -> new GithubAuth()),

        /**
         * Credentials type: env.
         */
        ENV((settings, mapping) -> new AuthFromEnv()),

        /**
         * Credentials type: keycloak.
         */
        KEYCLOAK((settings, mapping) -> new AuthFromKeycloak(
            new Configuration(
                mapping.string("url"),
                mapping.string("realm"),
                mapping.string("client-id"),
                Map.of("secret", mapping.string("client-password")),
                null
            )
        ));

        /**
         * Transform yaml to completion action with authentication.
         */
        private final BiFunction<YamlSettings, YamlMapping, Authentication> map;

        /**
         * Ctor.
         *
         * @param map Transform yaml to completion action with users.
         */
        CredentialsType(final BiFunction<YamlSettings, YamlMapping, Authentication> map) {
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
         * Transform yaml to completion action with authentication.
         *
         * @param settings YamlSettings.
         * @param yaml Credentials yaml mapping.
         * @return Completion action with users.
         */
        Authentication auth(
            final YamlSettings settings,
            final YamlMapping yaml
        ) {
            return this.map.apply(settings, yaml);
        }
    }
}
