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
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.rq.RequestLineFrom;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API authentication wrapper.
 * @since 0.6
 */
final class AuthApi implements Identities {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN_PATH = Pattern.compile("/api/repos/(?<user>[^/.]+)(?:/.*)?");

    /**
     * Origin authentication.
     */
    private final Authentication auth;

    /**
     * Wraps authentication with API restrictions.
     * @param auth Origin
     */
    AuthApi(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Optional<String> user(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        final Matcher matcher = PTN_PATH.matcher(new RequestLineFrom(line).uri().getPath());
        final Optional<String> res;
        if (matcher.matches()) {
            res = new BasicIdentities(this.auth)
                .user(line, headers)
                .filter(user -> user.equals(matcher.group("user")));
        } else {
            res = Optional.empty();
        }
        return res;
    }
}
