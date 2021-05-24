/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.ArtipieServer;

/**
 * Class to simplify working with url that may contains user credentials.
 * @since 0.13
 */
public final class RepositoryUrl {

    /**
     * Host, port, repo.
     */
    private final String hostportrepo;

    /**
     * Ctor.
     * @param port Port
     * @param reponame Reponame for url path
     */
    public RepositoryUrl(final int port, final String reponame) {
        this.hostportrepo = String.format("host.testcontainers.internal:%d/%s/", port, reponame);
    }

    /**
     * Simple url.
     * @return Simple url.
     */
    public String string() {
        return String.format("http://%s", this.hostportrepo);
    }

    /**
     * Url with or without user credentials.
     * @param anonymous Add credentials tor url or not
     * @return Url with credentials for default user.
     */
    public String string(final boolean anonymous) {
        final ArtipieServer.User user = ArtipieServer.ALICE;
        final String res;
        if (anonymous) {
            res = this.string();
        } else {
            res = this.string(user);
        }
        return res;
    }

    /**
     * Url with credentials.
     * @param user User with name and password
     * @return Url with credentials for specified user.
     */
    public String string(final ArtipieServer.User user) {
        return String.format(
            "http://%s:%s@%s",  user.name(), user.password(), this.hostportrepo
        );
    }
}
