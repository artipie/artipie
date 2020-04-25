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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.Npm;
import com.artipie.npm.http.NpmSlice;
import com.artipie.rpm.http.RpmSlice;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.reactivestreams.Publisher;

/**
 * Slice from repo config.
 * @since 0.1.4
 * @todo #90:30min We still don't have tests for Pie. But now that this class was extracted, we have
 *  a more cohesive class that could be tested. Write unit tests for SliceFromConfig class.
 */
public final class SliceFromConfig implements Slice {

    /**
     * Repository config.
     */
    private final RepoConfig config;

    /**
     * Ctor.
     * @param config Repo config
     */
    public SliceFromConfig(final RepoConfig config) {
        this.config = config;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        try {
            return SliceFromConfig.build(this.config).response(
                line, headers, body
            );
        } catch (final InterruptedException ex) {
            Logger.error(this, "Interruption when getting slice from config");
            throw new IllegalArgumentException(ex);
        } catch (final ExecutionException ex) {
            Logger.error(this, "Exception when getting slice from config");
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Find a slice implementation for config.
     * @param cfg Repository config
     * @return Slice
     * @throws ExecutionException If error getting the slice
     * @throws InterruptedException If error getting the slice
     */
    private static Slice build(final RepoConfig cfg) throws InterruptedException,
        ExecutionException {
        return cfg.type().thenCombine(
            cfg.storage(),
            (type, storage) -> {
                final Slice slice;
                switch (type) {
                    case "file":
                        slice = new FilesSlice(storage);
                        break;
                    case "npm":
                        slice = new NpmSlice(new Npm(storage), storage);
                        break;
                    case "gem":
                        slice = new GemSlice(storage);
                        break;
                    case "rpm":
                        slice = new RpmSlice(storage);
                        break;
                    case "php":
                        try {
                            slice = cfg.path().thenApply(
                                path -> new PhpComposer(path, storage)
                            ).toCompletableFuture().get();
                        } catch (final InterruptedException ex) {
                            Logger.error(SliceFromConfig.class, "Interrupted PhpComposer creation");
                            throw new IllegalArgumentException(ex);
                        } catch (final ExecutionException ex) {
                            Logger.error(SliceFromConfig.class, "Exception getting PhpComposer");
                            throw new IllegalArgumentException(ex);
                        }
                        break;
                    case "maven":
                        slice = new MavenSlice(storage);
                        break;
                    default:
                        throw new IllegalStateException(
                            String.format("Unsupported repository type '%s'", type)
                        );
                }
                return slice;
            }
        ).toCompletableFuture().get();
    }
}
