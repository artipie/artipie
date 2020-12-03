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

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.auth.CachedAuth;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.management.Users;
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
     * Ctor.
     * @param content YAML file content.
     */
    public YamlSettings(final YamlMapping content) {
        this.content = content;
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
            auth -> new Authentication.Joined(new CachedAuth(new GithubAuth()), auth)
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
}
