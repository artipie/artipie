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

import com.artipie.Settings;
import com.artipie.api.RsJson;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Health check slice.
 * <p>
 * Returns JSON with verbose status checks,
 * response status is {@code OK} if all status passed and {@code UNAVAILABLE} if any failed.
 * </p>
 * @since 0.10
 * @checkstyle AvoidInlineConditionalsCheck (500 lines)
 */
public final class HealthSlice implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New health slice.
     * @param settings Artipie settings
     */
    public HealthSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.storageStatus().thenApply(
                ok ->
                    new RsWithStatus(
                        new RsJson(
                            Json.createArrayBuilder().add(
                                Json.createObjectBuilder().add("storage", ok ? "ok" : "failure")
                            ).build()
                        ),
                        ok ? RsStatus.OK : RsStatus.UNAVAILABLE
                    )
            )
        );
    }

    /**
     * Checks storage status by writing {@code OK} to storage.
     * @return True if OK
     * @checkstyle ReturnCountCheck (10 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private CompletionStage<Boolean> storageStatus() {
        try {
            return this.settings.storage().save(
                new Key.From(".system", "test"),
                new Content.From("OK".getBytes(StandardCharsets.US_ASCII))
            ).thenApply(none -> true).exceptionally(ignore -> false);
        } catch (final IOException ignore) {
            return CompletableFuture.completedFuture(false);
        }
    }
}
