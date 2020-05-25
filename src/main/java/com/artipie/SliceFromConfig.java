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

import com.artipie.asto.Storage;
import com.artipie.auth.AuthFromEnv;
import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.gem.GemSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.Npm;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.NpmProxyConfig;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.pypi.PySlice;
import com.artipie.rpm.http.RpmSlice;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Slice from repo config.
 * @since 0.1.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 */
public final class SliceFromConfig extends Slice.Wrap {

    /**
     * Ctor.
     * @param config Repo config
     * @param vertx Vertx instance
     */
    public SliceFromConfig(final RepoConfig config, final Vertx vertx) {
        super(
            new AsyncSlice(SliceFromConfig.build(config, vertx, new AuthFromEnv()))
        );
    }

    /**
     * Find a slice implementation for config.
     * @param cfg Repository config
     * @param vertx Vertx instance
     * @param auth Authentication implementation
     * @return Slice completionStage
     * @todo #90:30min This method still needs more refactoring.
     *  We should test if the type exist in the constructed map. If the type does not exist,
     *  we should throw an IllegalStateException with the message "Unsupported repository type '%s'"
     * @checkstyle LineLengthCheck (100 lines)
     */
    static CompletionStage<Slice> build(final RepoConfig cfg, final Vertx vertx,
        final Authentication auth) {
        return cfg.type().thenCompose(
            type -> cfg.storage().thenCombine(
                cfg.permissions(),
                (storage, permissions) -> new MapOf<String, Function<RepoConfig, CompletionStage<Slice>>>(
                    new MapEntry<>(
                        "file", config -> CompletableFuture.completedStage(new FilesSlice(storage, permissions, auth))
                    ),
                    new MapEntry<>(
                        "npm", config -> CompletableFuture.completedStage(
                        new NpmSlice(new Npm(storage), storage)
                    )
                    ),
                    new MapEntry<>(
                        "gem", config -> CompletableFuture.completedStage(new GemSlice(storage, vertx.fileSystem()))
                    ),
                    new MapEntry<>(
                        "rpm", config -> CompletableFuture.completedStage(new RpmSlice(storage))
                    ),
                    new MapEntry<>(
                        "php",
                        config -> php(config, storage)
                    ),
                    new MapEntry<>(
                        "nuget",
                        config -> nuGet(cfg, storage)
                    ),
                    new MapEntry<>(
                        "maven", config -> CompletableFuture.completedStage(new MavenSlice(storage, permissions, auth))
                    ),
                    new MapEntry<>(
                        "go", config -> CompletableFuture.completedStage(new GoSlice(storage))
                    ),
                    new MapEntry<>(
                        "npm-proxy",
                        config -> config.path().thenCombine(
                            config.settings(),
                            (path, settings) -> new NpmProxySlice(
                                path,
                                new NpmProxy(
                                    new NpmProxyConfig(settings.orElseThrow()),
                                    vertx,
                                    storage
                                )
                            )
                        )
                    ),
                    new MapEntry<>(
                        "pypi", config -> pypi(cfg, storage)
                    )
                ).get(type).apply(cfg)
            ).thenCompose(Function.identity()).thenCompose(
                slice -> cfg.contentLengthMax().thenApply(
                    opt -> opt.<Slice>map(limit -> new ContentLengthRestriction(slice, limit))
                        .orElse(slice)
                )
            )
        );
    }

    /**
     * Creates PHP Composer slice.
     *
     * @param config Repository config.
     * @param storage Storage.
     * @return Slice instance.
     */
    private static CompletionStage<Slice> php(final RepoConfig config, final Storage storage) {
        return config.path().thenApply(
            path -> new PhpComposer(path, storage)
        );
    }

    /**
     * Creates Python slice.
     *
     * @param config Repository config.
     * @param storage Storage.
     * @return Slice instance.
     */
    private static CompletionStage<Slice> pypi(final RepoConfig config, final Storage storage) {
        return config.path().thenApply(
            path -> new PySlice(path, storage)
        );
    }

    /**
     * Creates NuGet slice.
     *
     * @param config Repository config.
     * @param storage Storage.
     * @return Slice instance.
     */
    private static CompletionStage<Slice> nuGet(final RepoConfig config, final Storage storage) {
        return config.url().thenCompose(
            url -> config.path().thenApply(
                path -> new NuGet(url, path, storage)
            )
        );
    }
}
