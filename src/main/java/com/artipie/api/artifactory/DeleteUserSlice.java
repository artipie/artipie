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

import com.artipie.Users;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Artifactory `DELETE /api/security/users/{userName}` endpoint,
 * deletes user record from credentials.
 *
 * @since 0.10
 */
public final class DeleteUserSlice implements Slice {
    /**
     * Artipie users.
     */
    private final Users users;

    /**
     * Ctor.
     * @param users Users
     */
    public DeleteUserSlice(final Users users) {
        this.users = users;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> user = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return user.<Response>map(
            username -> new AsyncResponse(
                this.users.list().thenApply(
                    items -> items.stream().anyMatch(item -> item.name().equals(username))
                ).thenCompose(
                    has -> {
                        final CompletionStage<Response> resp;
                        if (has) {
                            resp = this.users.remove(username)
                                .thenApply(
                                    ok -> new RsWithBody(
                                        new RsWithStatus(RsStatus.OK),
                                        String.format(
                                            "User '%s' has been removed successfully.",
                                            username
                                        ).getBytes(StandardCharsets.UTF_8)
                                    )
                                );
                        } else {
                            resp = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                        }
                        return resp;
                    }
                )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }
}
