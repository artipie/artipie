/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.auth.AuthCache;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.management.Users;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 * @todo #285:30min Settings configuration for GitHub auth.
 *  Add additional settings configuration for GitHub authentication,
 *  now it's applied by default to auth from yaml using chained authentication, see auth()
 *  method. We can configure this chain via settings and compose complex authentication
 *  providers there. E.g. user can use ordered list of env auth, github auth
 *  and auth from yaml file.
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class YamlSettings implements Settings {

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * Authentication cache.
     */
    private final AuthCache authcache;

    /**
     * Ctor.
     * @param content YAML file content.
     * @param authcache Auth cache
     */
    public YamlSettings(final YamlMapping content, final AuthCache authcache) {
        this.content = content;
        this.authcache = authcache;
    }

    @Override
    public Storage storage() {
        return new MeasuredStorage(
            new YamlStorage(this.meta().yamlMapping("storage"))
                .storage()
        );
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return this.credentials().thenCompose(
            Users::auth
        ).thenApply(
            auth -> new Authentication.Joined(
                new Cached(this.authcache, new GithubAuth()),
                auth
            )
        );
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
        final YamlMapping cred = this.meta()
            .yamlMapping("credentials");
        final CompletionStage<Users> res;
        final String path = "path";
        if (YamlSettings.hasTypeFile(cred) && cred.string(path) != null) {
            final Storage strg = this.storage();
            final KeyFromPath key = new KeyFromPath(cred.string(path));
            res = strg.exists(key).thenApply(
                exists -> {
                    final Users creds;
                    if (exists) {
                        creds = new UsersFromStorageYaml(strg, key);
                    } else {
                        creds = new UsersFromEnv();
                    }
                    return creds;
                }
            );
        } else if (YamlSettings.hasTypeFile(cred)) {
            res = CompletableFuture.failedFuture(
                new RuntimeException(
                    "Invalid credentials configuration: type `file` requires `path`!"
                )
            );
        } else {
            res = CompletableFuture.completedStage(new UsersFromEnv());
        }
        return res;
    }

    /**
     * Check that yaml has `type: file` mapping in the credentials setting.
     * @param cred Credentials yaml section
     * @return True if setting is present
     */
    private static boolean hasTypeFile(final YamlMapping cred) {
        return cred != null && "file".equals(cred.string("type"));
    }

    /**
     * Wrapping for auth cache.
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
        public Optional<User> user(final String username, final String password) {
            return this.cache.user(username, password, this.origin);
        }
    }
}
