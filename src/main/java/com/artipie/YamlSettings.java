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
import com.artipie.asto.Storage;
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromYaml;
import com.artipie.auth.CachedAuth;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import java.io.IOException;
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
 * @todo #444:30min Move `auth()` method to Credentials interface
 *  Casting in `auth()` may cause problems in runtime, it would be better to move this method to
 *  Credentials interface. Credentials.FromStorageYaml implementation should create
 *  AuthFromYaml instance in `auth()`, and as we have env credentials, let's introduce
 *  Credentials.FromEnv class to create proper Authentication implementation in `auth()`.
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class YamlSettings implements Settings {

    /**
     * Meta section.
     */
    private static final String KEY_META = "meta";

    /**
     * YAML file content.
     */
    private final String content;

    /**
     * Ctor.
     * @param content YAML file content.
     */
    public YamlSettings(final String content) {
        this.content = content;
    }

    @Override
    public Storage storage() throws IOException {
        return new MeasuredStorage(
            new YamlStorage(
                Yaml.createYamlInput(this.content)
                    .readYamlMapping()
                    .yamlMapping(YamlSettings.KEY_META)
                    .yamlMapping("storage")
            ).storage()
        );
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return this.credentials().thenCompose(
            opt -> opt.<CompletionStage<Authentication>>map(
                creds -> ((Credentials.FromStorageYaml) creds).yaml().thenApply(AuthFromYaml::new)
            ).orElse(CompletableFuture.completedFuture(new AuthFromEnv()))
        ).thenApply(
            auth -> new Authentication.Joined(new CachedAuth(new GithubAuth()), auth)
        );
    }

    @Override
    public String layout() throws IOException {
        return Yaml.createYamlInput(this.content)
            .readYamlMapping()
            .yamlMapping(YamlSettings.KEY_META)
            .string("layout");
    }

    @Override
    public YamlMapping meta() throws IOException {
        return Yaml.createYamlInput(this.content)
            .readYamlMapping()
            .yamlMapping(YamlSettings.KEY_META);
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Optional<Credentials>> credentials() {
        final YamlMapping cred;
        try {
            cred = Yaml.createYamlInput(this.content)
                .readYamlMapping()
                .yamlMapping(YamlSettings.KEY_META)
                .yamlMapping("credentials");
        } catch (final IOException err) {
            return CompletableFuture.failedFuture(err);
        }
        final CompletionStage<Optional<Credentials>> res;
        final String path = "path";
        if (YamlSettings.hasTypeFile(cred) && cred.string(path) != null) {
            final Storage strg;
            try {
                strg = this.storage();
            } catch (final IOException err) {
                return CompletableFuture.failedFuture(err);
            }
            final KeyFromPath key = new KeyFromPath(cred.string(path));
            res = strg.exists(key).thenApply(
                exists -> {
                    final Optional<Credentials> auth;
                    if (exists) {
                        auth = Optional.of(new Credentials.FromStorageYaml(strg, key));
                    } else {
                        auth = Optional.empty();
                    }
                    return auth;
                }
            );
        } else if (YamlSettings.hasTypeFile(cred)) {
            res = CompletableFuture.failedFuture(
                new RuntimeException(
                    "Invalid credentials configuration: type `file` requires `path`!"
                )
            );
        } else {
            res = CompletableFuture.completedStage(Optional.empty());
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
}
