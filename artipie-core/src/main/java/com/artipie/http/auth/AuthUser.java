/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Objects;

/**
 * Authenticated user.
 *
 * @since 0.16
 */
public final class AuthUser {

    /**
     * Anonymous user is a user who doesn't have any credentials.
     */
    public static final AuthUser ANONYMOUS = new AuthUser("anonymous", "unknown");

    /**
     * User name.
     */
    private final String uname;

    /**
     * Authentication context.
     */
    private final String context;

    /**
     * Ctor.
     *
     * @param name Name of the user
     * @param context Authentication context
     */
    public AuthUser(final String name, final String context) {
        this.uname = Objects.requireNonNull(name, "User's name cannot be null!");
        this.context = context;
    }

    /**
     * Ctor with test context for usages in tests.
     *
     * @param name Name of the user
     */
    AuthUser(final String name) {
        this(name, "test");
    }

    /**
     * Get user's name.
     *
     * @return Name
     */
    public String name() {
        return this.uname;
    }

    /**
     * Returns authentication context.
     * @return Context string
     */
    public String authContext() {
        return this.context;
    }

    /**
     * This user is anonymous.
     *
     * @return True if user is anonymous.
     */
    public boolean isAnonymous() {
        return this.uname.equals(AuthUser.ANONYMOUS.uname);
    }

    @Override
    public boolean equals(final Object other) {
        final boolean res;
        if (this == other) {
            res = true;
        } else if (other == null || this.getClass() != other.getClass()) {
            res = false;
        } else {
            final AuthUser user = (AuthUser) other;
            res = Objects.equals(this.uname, user.uname);
        }
        return res;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uname);
    }

    @Override
    public String toString() {
        return String.format(
            "%s(%s)",
            this.getClass().getSimpleName(),
            this.uname
        );
    }
}
