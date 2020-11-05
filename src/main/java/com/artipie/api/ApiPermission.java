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
import com.artipie.http.auth.Permission;
import com.artipie.http.rq.RequestLineFrom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Permissions for API and dashboard endpoints.
 * Accepts HTTP request line as action and checks that request is allowed for the user.
 *
 * @since 0.13
 */
final class ApiPermission implements Permission {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN_PATH =
        Pattern.compile("(?:/api/\\w+|/dashboard)?/(?<user>[^/.]+)(?:/.*)?");

    /**
     * HTTP request line.
     */
    private final String line;

    /**
     * Ctor.
     *
     * @param line HTTP request line.
     */
    ApiPermission(final String line) {
        this.line = line;
    }

    @Override
    public boolean allowed(final Authentication.User user) {
        final Matcher matcher = PTN_PATH.matcher(new RequestLineFrom(this.line).uri().getPath());
        return matcher.matches() && user.name().equals(matcher.group("user"));
    }
}
