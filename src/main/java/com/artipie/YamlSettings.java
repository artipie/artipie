/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.cache.AuthCache;
import com.artipie.cache.SettingsCaches;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.management.Users;
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
 * @todo #337:30min Add a description of alternative authentication.
 *  Add a description of alternative authentication to the README file.
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class YamlSettings implements Settings {

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * A set of caches for settings.
     */
    private final SettingsCaches caches;

    /**
     * Ctor.
     * @param content YAML file content.
     * @param caches Settings caches
     */
    public YamlSettings(final YamlMapping content, final SettingsCaches caches) {
        this.content = content;
        this.caches = caches;
    }

    @Override
    public Storage storage() {
        return this.caches.storageConfig().storage(this);
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return this.credentials().thenCompose(
            Users::auth
        ).thenCompose(this::withAlternative);
    }

    @Override
    public Layout layout() {
        final Layout result;
        final String name = this.meta().string("layout");
        if (name == null || name.equals("flat")) {
            result = new Layout.Flat();
        } else {
            result = new Layout.Org();
        }
        return result;
    }

    @Override
    public YamlMapping meta() {
        return this.content.yamlMapping("meta");
    }

    @Override
    public Storage repoConfigsStorage() {
        return Optional.ofNullable(this.meta().string("repo_configs"))
            .<Storage>map(str -> new SubStorage(new Key.From(str), this.storage()))
            .orElse(this.storage());
    }

    @Override
    public CompletionStage<Users> credentials() {
        return this.credentials(this.credentialsYaml());
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

    /**
     * Get credentials yaml mapping.
     *
     * @return Credentials yaml mapping.
     */
    private YamlMapping credentialsYaml() {
        return this.meta().yamlMapping("credentials");
    }

    /**
     * Parse credentials from yaml.
     *
     * @param cred Credentials yaml mapping
     * @return Completion action with users.
     */
    private CompletionStage<Users> credentials(final YamlMapping cred) {
        return CredentialsType.valueOf(cred).users(this, cred);
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
        final String alternative = "alternative_auth";
        final YamlMapping cred = this.credentialsYaml();
        if (cred != null) {
            YamlMapping alt = cred.yamlMapping(alternative);
            while (alt != null) {
                final CompletionStage<Users> users = this.credentials(alt);
                res = res.thenCompose(
                    prev -> users
                        .thenCompose(Users::auth)
                        .thenApply(next -> new Authentication.Joined(prev, next))
                );
                alt = alt.yamlMapping(alternative);
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
            final String path = mapping.string("path");
            if (path != null) {
                final Storage storage = settings.storage();
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
            new UsersWithCachedAuth(settings.caches.auth(), new UsersFromGithub())
        )),

        /**
         * Credentials type: env.
         */
        ENV((settings, mapping) -> CompletableFuture.completedStage(
            new UsersFromEnv()
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
                    yaml.string("type").toUpperCase(Locale.getDefault())
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

    /**
     * Wrapping for auth cache.
     *
     * @since 0.22
     */
    private static final class Cached implements Authentication {
        /**
         * Auth cache.
         */
        private final AuthCache cache;

        /**
         * Auth provider.
         */
        private final Authentication origin;

        /**
         * Ctor.
         * @param cache Auth cache
         * @param origin Auth provider
         */
        Cached(final AuthCache cache, final Authentication origin) {
            this.cache = cache;
            this.origin = origin;
        }

        @Override
        public Optional<User> user(
            final String username,
            final String password
        ) {
            return this.cache.user(username, password, this.origin);
        }

        @Override
        public String toString() {
            return String.format(
                "%s(%s)",
                this.getClass().getSimpleName(),
                this.origin
            );
        }
    }

    /**
     * Wrapping for artipie credentials that produce a cached authentication.
     *
     * @since 0.22
     */
    private static final class UsersWithCachedAuth implements Users {
        /**
         * Auth cache.
         */
        private final AuthCache cache;

        /**
         * Original users.
         */
        private final Users users;

        /**
         * Ctor.
         *
         * @param cache Auth cache.
         * @param users Artipie credentials.
         */
        UsersWithCachedAuth(final AuthCache cache, final Users users) {
            this.cache = cache;
            this.users = users;
        }

        @Override
        public CompletionStage<List<User>> list() {
            return this.users.list();
        }

        @Override
        public CompletionStage<Void> add(
            final User user,
            final String pswd,
            final PasswordFormat format
        ) {
            return this.users.add(user, pswd, format);
        }

        @Override
        public CompletionStage<Void> remove(final String username) {
            return this.users.remove(username);
        }

        @Override
        public CompletionStage<Authentication> auth() {
            return this.users.auth().thenApply(auth -> new Cached(this.cache, auth));
        }
    }
}
