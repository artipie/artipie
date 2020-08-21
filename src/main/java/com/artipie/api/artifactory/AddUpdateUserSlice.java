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
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.reactivestreams.Publisher;

/**
 * Artifactory `PUSH/PUT /api/security/users/{userName}` endpoint,
 * adds/updates user record in credentials.
 * @since 0.10
 * @todo #444:30min Implement this slice to add/update user from credentials by user name
 *  obtained from request line, path format is `/api/security/users/{userName}`. Password should
 *  be obtain from `password` field from json request body.
 *  Use Credentials#add(java.lang.String, java.lang.String) method to perform the operation and
 *  return 200 OK status. Do not forget to test this class and add it to ArtipieApi,
 *  check GetUserSlice as an example.
 */
public final class AddUpdateUserSlice implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Artipie setting
     */
    public AddUpdateUserSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response res;
        final Matcher matcher = GetUserSlice.PTRN.matcher(
            new RequestLineFrom(line).uri().toString()
        );
        if (matcher.matches()) {
            final String pswd = AddUpdateUserSlice.password(body);
            final String username = matcher.group("username");
            res = new AsyncResponse(
                this.settings.credentials().thenApply(
                    cred -> cred.add(username, pswd)
                        .thenApply(ok -> new RsWithStatus(RsStatus.OK))
                        .toCompletableFuture()
                        .join()
                    )
                );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Extracts password string from the response body.
     * @param body Response body
     * @return Password string.
     */
    private static String password(final Publisher<ByteBuffer> body) {
        final JsonObject root;
        final byte[] bytes = new PublisherAs(body)
            .bytes().toCompletableFuture().join();
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            root = reader.readObject();
            return root.getString("password");
        }
    }
}
