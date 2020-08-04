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
package com.artipie.api.artifactory;

import com.artipie.Settings;
import com.artipie.api.ContentAs;
import com.artipie.asto.Key;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.jcabi.log.Logger;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * Artifactory create repo API slice, it accepts json and create new docker repository by
 * creating corresponding YAML configuration.
 * @since 0.9
 */
public final class CreateRepoSlice implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Artipie settings
     */
    public CreateRepoSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        // @checkstyle ReturnCountCheck (20 lines)
        return new AsyncResponse(
            Single.just(body).to(ContentAs.JSON).flatMap(
                json -> Single.fromFuture(
                    valid(json).map(
                        name -> {
                            try {
                                return this.settings.storage().exists(
                                    new Key.From(String.format("%s.yaml", name))
                                ).thenApply(
                                    exists -> {
                                        final Response res;
                                        if (exists) {
                                            res = new RsWithStatus(RsStatus.BAD_REQUEST);
                                        } else {
                                            res = new RsWithStatus(RsStatus.OK);
                                        }
                                        return res;
                                    }
                                );
                            } catch (final IOException ex) {
                                Logger.error(this, ex.toString());
                                return CompletableFuture.completedFuture(
                                    new RsWithStatus(RsStatus.INTERNAL_ERROR)
                                );
                            }
                        }
                    ).orElse(
                        CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST))
                    )
                )
            )
        );
    }

    /**
     * Checks if json is valid (contains new repo key and supported setting) and
     * return new repo name.
     * @param json Json to read repo name from
     * @return True if json is correct
     */
    private static Optional<String> valid(final JsonObject json) {
        final Optional<String> res;
        final String key = json.getString("key", "");
        if (!key.isEmpty() && "local".equals(json.getString("rclass", ""))
            && "docker".equals(json.getString("packageType", ""))
            && "V2".equals(json.getString("dockerApiVersion", ""))) {
            res = Optional.of(key);
        } else {
            res = Optional.empty();
        }
        return res;
    }

}
