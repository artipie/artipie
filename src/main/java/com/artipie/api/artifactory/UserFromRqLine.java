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

import com.artipie.http.rq.RequestLineFrom;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for receiving username from the request line.
 * The request line should match pattern to get username.
 * @since 0.10
 */
final class UserFromRqLine {
    /**
     * Request line pattern to get username.
     */
    private static final Pattern PTRN = Pattern.compile("/api/security/users/(?<username>[^/.]+)");

    /**
     * Request line.
     */
    private final String rqline;

    /**
     * Ctor.
     * @param rqline Request line
     */
    UserFromRqLine(final String rqline) {
        this.rqline = rqline;
    }

    /**
     * Get username if the request line matches pattern to get username.
     * @return Username from the request line.
     */
    Optional<String> get() {
        final Optional<String> username;
        final Matcher matcher = UserFromRqLine.PTRN.matcher(
            new RequestLineFrom(this.rqline).uri().toString()
        );
        if (matcher.matches()) {
            username = Optional.of(matcher.group("username"));
        } else {
            username = Optional.empty();
        }
        return username;
    }
}
