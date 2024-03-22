/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.settings.Settings;

import javax.json.Json;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Health check slice.
 * <p>
 * Returns JSON with verbose status checks,
 * response status is {@code OK} if all status passed and {@code UNAVAILABLE} if any failed.
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
    public Response response(RequestLine line, Headers headers, Content body) {
        return new AsyncResponse(
            this.storageStatus()
                .thenApply(
                    ok -> {
                        if (ok) {
                            return BaseResponse.ok()
                                .jsonBody(Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder().add("storage", "ok"))
                                    .build()
                                );
                        }
                        return BaseResponse.unavailable()
                            .jsonBody(Json.createArrayBuilder().add(
                                Json.createObjectBuilder().add("storage", "failure")
                            ).build());
                    }
            )
        );
    }

    /**
     * Checks storage status by writing {@code OK} to storage.
     * @return True if OK
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private CompletionStage<Boolean> storageStatus() {
        try {
            return this.settings.configStorage().save(
                new Key.From(".system", "test"),
                new Content.From("OK".getBytes(StandardCharsets.US_ASCII))
            ).thenApply(none -> true).exceptionally(ignore -> false);
        } catch (final Exception ignore) {
            return CompletableFuture.completedFuture(false);
        }
    }
}
