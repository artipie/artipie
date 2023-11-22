/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import com.artipie.settings.Settings;
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
@SuppressWarnings("PMD.AvoidCatchingGenericException")
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
            return this.settings.configStorage().save(
                new Key.From(".system", "test"),
                new Content.From("OK".getBytes(StandardCharsets.US_ASCII))
            ).thenApply(none -> true).exceptionally(ignore -> false);
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ignore) {
            return CompletableFuture.completedFuture(false);
        }
    }
}
