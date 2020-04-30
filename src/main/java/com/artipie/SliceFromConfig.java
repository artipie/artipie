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

import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.gem.GemSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.Npm;
import com.artipie.npm.http.NpmSlice;
import com.artipie.rpm.http.RpmSlice;
import io.vertx.reactivex.core.file.FileSystem;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Slice from repo config.
 * @since 0.1.4
 * @todo #90:30min We still don't have tests for Pie. But now that this class was extracted, we have
 *  a more cohesive class that could be tested. Write unit tests for SliceFromConfig class.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 */
public final class SliceFromConfig extends Slice.Wrap {

    /**
     * Ctor.
     * @param config Repo config
     * @param fs The file system
     */
    public SliceFromConfig(final RepoConfig config, final FileSystem fs) {
        super(
            new AsyncSlice(SliceFromConfig.build(config, fs))
        );
    }

    /**
     * Find a slice implementation for config.
     * @param cfg Repository config
     * @param fs The file system
     * @return Slice completionStage
     * @todo #90:30min This method still needs more refactoring.
     *  We should test if the type exist in the constructed map. If the type does not exist,
     *  we should throw an IllegalStateException with the message "Unsupported repository type '%s'"
     */
    private static CompletionStage<Slice> build(final RepoConfig cfg, final FileSystem fs) {
        return cfg.type().thenCombine(
            cfg.storage(),
            (type, storage) -> {
                return new MapOf<String, Function<RepoConfig, CompletionStage<Slice>>>(
                    new MapEntry<>(
                        "file", config -> CompletableFuture.completedStage(new FilesSlice(storage))
                    ),
                    new MapEntry<>(
                        "npm", config -> CompletableFuture.completedStage(
                            new NpmSlice(new Npm(storage), storage)
                        )
                    ),
                    new MapEntry<>(
                        "gem", config -> CompletableFuture.completedStage(new GemSlice(storage, fs))
                    ),
                    new MapEntry<>(
                        "rpm", config -> CompletableFuture.completedStage(new RpmSlice(storage))
                    ),
                    new MapEntry<>(
                        "php",
                        config -> {
                            return config.path().thenApply(
                                path -> new PhpComposer(path, storage)
                            );
                        }
                    ),
                    new MapEntry<>(
                        "maven", config -> CompletableFuture.completedStage(new MavenSlice(storage))
                    ),
                    new MapEntry<>(
                        "go", config -> CompletableFuture.completedStage(new GoSlice(storage))
                    )
                ).get(type).apply(cfg);
            }
        ).thenCompose(Function.identity());
    }
}
