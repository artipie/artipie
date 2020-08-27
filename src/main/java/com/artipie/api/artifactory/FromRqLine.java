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
 * Class for receiving value from the request line.
 * The request line should match pattern to get value.
 * @since 0.10
 */
public final class FromRqLine {
    /**
     * Request line pattern to get username.
     */
    public static final Pattern PTRN_USER = Pattern.compile(
        "/api/security/users/(?<username>[^/.]+)"
    );

    /**
     * Request line pattern to get repo.
     */
    public static final Pattern PTRN_REPO = Pattern.compile(
        "/api/security/permissions/(?<repo>[^/.]+)"
    );

    /**
     * Request line.
     */
    private final String rqline;

    /**
     * Group name.
     */
    private final Group mtchgroup;

    /**
     * Request line pattern to get value.
     */
    private final Pattern ptrn;

    /**
     * Ctor.
     * @param rqline Request line
     * @param mtchgroup Group name
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    FromRqLine(final String rqline, final Group mtchgroup) {
        this.rqline = rqline;
        this.mtchgroup = mtchgroup;
        switch (mtchgroup) {
            case USER:
                this.ptrn = FromRqLine.PTRN_USER;
                break;
            case REPO:
                this.ptrn = FromRqLine.PTRN_REPO;
                break;
            default:
                throw new IllegalArgumentException("Couldn't find suitable matcher group.");
        }
    }

    /**
     * Get username if the request line matches pattern to get username.
     * @return Username from the request line.
     */
    Optional<String> get() {
        final Optional<String> username;
        final Matcher matcher = this.ptrn.matcher(
            new RequestLineFrom(this.rqline).uri().toString()
        );
        if (matcher.matches()) {
            username = Optional.of(matcher.group(this.mtchgroup.group()));
        } else {
            username = Optional.empty();
        }
        return username;
    }

    /**
     * Group name for part of the request line.
     */
    enum Group {
        /**
         * Username group.
         */
        USER("username"),

        /**
         * Repo group.
         */
        REPO("repo");

        /**
         * Group value.
         */
        private final String string;

        /**
         * Ctor.
         *
         * @param string Group value.
         */
        Group(final String string) {
            this.string = string;
        }

        /**
         * Value as group name.
         *
         * @return Group name string.
         */
        public String group() {
            return this.string;
        }

    }
}
