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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.reactivestreams.Publisher;

/**
 * Artifactory `PUSH/PUT /api/security/users/{userName}` endpoint,
 * updates/adds user record in credentials.
 *
 * @since 0.10
 */
public final class AddUpdateUserSlice implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     *
     * @param settings Artipie setting
     */
    public AddUpdateUserSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> user = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return user.<Response>map(
            username -> new AsyncResponse(
                AddUpdateUserSlice.password(body)
                    .thenCompose(
                        passw -> passw.map(
                            haspassw -> this.settings.credentials()
                                .thenCompose(
                                    cred -> cred.add(username, haspassw)
                                        .thenApply(ok -> new RsWithStatus(RsStatus.OK)))
                ).orElse(CompletableFuture.completedFuture(new RsWithStatus(RsStatus.NOT_FOUND))))
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }

    /**
     * Extracts password string from the response body.
     *
     * @param body Response body
     * @return Password string as completion.
     */
    private static CompletionStage<Optional<String>> password(final Publisher<ByteBuffer> body) {
        return new PublisherAs(body)
            .bytes().thenApply(
                bytes -> {
                    final Optional<String> pswd;
                    if (bytes.length == 0) {
                        pswd = Optional.empty();
                    } else {
                        final JsonReader rdr = Json.createReader(new ByteArrayInputStream(bytes));
                        final JsonObject root = rdr.readObject();
                        pswd = Optional.of(root.getString("password"));
                    }
                    return pswd;
                }
            );
    }
}
