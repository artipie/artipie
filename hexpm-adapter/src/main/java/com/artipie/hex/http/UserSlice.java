/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;

import java.util.regex.Pattern;

/**
 * This slice returns content about user in erlang format.
 */
public final class UserSlice implements Slice {
    /**
     * Path to users.
     */
    static final Pattern USERS = Pattern.compile("/users/(?<user>\\S+)");

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return BaseResponse.noContent();
    }
}
