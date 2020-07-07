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
package com.artipie.http;

import com.artipie.RepoConfig;
import com.artipie.Settings;
import com.artipie.SliceFromConfig;
import com.artipie.StorageAliases;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories implementation.
 * @since 0.9
 */
final class ArtipieRepositories implements Repositories {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New Artipie repositories.
     * @param settings Artipie settings
     */
    ArtipieRepositories(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Slice slice(final Key name) throws IOException {
        final Storage storage = this.settings.storage();
        final Key.From key = new Key.From(String.format("%s.yaml", name.string()));
        return new AsyncSlice(
            storage.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Slice> res;
                    if (exists) {
                        res = storage.value(key).thenCompose(
                            pub -> StorageAliases.find(storage, name).thenCompose(
                                aliases -> RepoConfig.fromPublisher(aliases, name, pub).thenApply(
                                    cfg -> new SliceFromConfig(this.settings, cfg, aliases)
                                )
                            )
                        );
                    } else {
                        res = CompletableFuture.completedFuture(
                            new SliceSimple(
                                new RsWithBody(
                                    StandardRs.NOT_FOUND,
                                    String.format("Repository '%s' not found", name.string()),
                                    StandardCharsets.UTF_8
                                )
                            )
                        );
                    }
                    return res;
                }
            )
        );
    }
}
