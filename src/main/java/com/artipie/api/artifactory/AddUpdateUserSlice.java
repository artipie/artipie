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
import com.artipie.Users;
import com.artipie.api.ContentAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
                AddUpdateUserSlice.info(body)
                    .thenCompose(
                        json -> json.map(
                            info -> this.settings.credentials()
                                .thenCompose(
                                    cred -> cred.add(
                                        new Users.User(
                                            username, Optional.of(info.getValue())
                                        ),
                                        DigestUtils.sha256Hex(info.getKey()),
                                        Users.PasswordFormat.SHA256
                                    ).thenApply(ok -> new RsWithStatus(RsStatus.OK))
                                )
                ).orElse(CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST))))
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }

    /**
     * Extracts password and email from the request body.
     *
     * @param body Request body
     * @return Password and email as completion.
     */
    private static CompletionStage<Optional<Pair<String, String>>> info(
        final Publisher<ByteBuffer> body
    ) {
        final String email = "email";
        final String pswd = "password";
        return Single.just(body).to(ContentAs.JSON).map(
            json -> {
                final Optional<Pair<String, String>> res;
                if (json.containsKey(pswd) && json.containsKey(email)) {
                    res = Optional.of(
                        new ImmutablePair<>(
                            json.getString(pswd), json.getString(email)
                        )
                    );
                } else {
                    res = Optional.empty();
                }
                return res;
            }
        ).to(SingleInterop.get());
    }
}
