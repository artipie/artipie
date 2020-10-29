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
package com.artipie.test;

import com.artipie.ArtipieServer;

/**
 * Class to simplify working with url that may contains user credentials.
 * @since 0.12
 */
public final class UrlCredsHelper {

    /**
     * Host, port, repo.
     */
    private final String hostportrepo;

    /**
     * Ctor.
     * @param port Port
     * @param reponame Reponame for url path
     */
    public UrlCredsHelper(final int port, final String reponame) {
        this.hostportrepo = String.format("host.testcontainers.internal:%d/%s/", port, reponame);
    }

    /**
     * Simple url.
     * @return Simple url.
     */
    public String url() {
        return String.format("http://%s", this.hostportrepo);
    }

    /**
     * Url with or without user credentials.
     * @param anonymous Add credentials tor url or not
     * @return Url with credentials for default user.
     */
    public String url(final boolean anonymous) {
        final ArtipieServer.User user = ArtipieServer.ALICE;
        final String res;
        if (anonymous) {
            res = this.url();
        } else {
            res = this.url(user);
        }
        return res;
    }

    /**
     * Url with credentials.
     * @param user User with name and password
     * @return Url with credentials for specified user.
     */
    public String url(final ArtipieServer.User user) {
        final String creds = String.format(
            "%s:%s@", user.name(), user.password()
        );
        return String.format(
            "http://%s%s", creds, this.hostportrepo
        );
    }
}
