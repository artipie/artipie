/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * This slice returns content about user in erlang format.
 *
 * @since 0.1
 */
public final class UserSlice implements Slice {
    /**
     * Path to users.
     */
    static final Pattern USERS = Pattern.compile("/users/(?<user>\\S+)");

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsWithStatus(RsStatus.NO_CONTENT);
    }
}
