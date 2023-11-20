/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Authentication mechanism to verify user.
 * @since 0.8
 */
public interface Authentication {

    /**
     * Resolve anyone as an anonymous user.
     */
    Authentication ANONYMOUS = (name, pswd) -> Optional.of(new AuthUser("anonymous", "unknown"));

    /**
     * Any user instance.
     */
    AuthUser ANY_USER = new AuthUser("*", "any");

    /**
     * Find user by credentials.
     * @param username Username
     * @param password Password
     * @return User login if found
     */
    Optional<AuthUser> user(String username, String password);

    /**
     * Abstract decorator for Authentication.
     *
     * @since 0.15
     */
    abstract class Wrap implements Authentication {

        /**
         * Origin authentication.
         */
        private final Authentication auth;

        /**
         * Ctor.
         *
         * @param auth Origin authentication.
         */
        protected Wrap(final Authentication auth) {
            this.auth = auth;
        }

        @Override
        public final Optional<AuthUser> user(final String username, final String password) {
            return this.auth.user(username, password);
        }
    }

    /**
     * Authentication implementation aware of single user with specified password.
     *
     * @since 0.15
     */
    final class Single implements Authentication {

        /**
         * User.
         */
        private final AuthUser user;

        /**
         * Password.
         */
        private final String password;

        /**
         * Ctor.
         *
         * @param user Username.
         * @param password Password.
         */
        public Single(final String user, final String password) {
            this(new AuthUser(user, "single"), password);
        }

        /**
         * Ctor.
         *
         * @param user User
         * @param password Password
         */
        public Single(final AuthUser user, final String password) {
            this.user = user;
            this.password = password;
        }

        @Override
        public Optional<AuthUser> user(final String name, final String pass) {
            return Optional.of(name)
                .filter(item -> item.equals(this.user.name()))
                .filter(ignored -> this.password.equals(pass))
                .map(ignored -> this.user);
        }
    }

    /**
     * Joined authentication composes multiple authentication instances into single one.
     * User authenticated if any of authentication instances authenticates the user.
     *
     * @since 0.16
     */
    final class Joined implements Authentication {

        /**
         * Origin authentications.
         */
        private final List<Authentication> origins;

        /**
         * Ctor.
         *
         * @param origins Origin authentications.
         */
        public Joined(final Authentication... origins) {
            this(Arrays.asList(origins));
        }

        /**
         * Ctor.
         *
         * @param origins Origin authentications.
         */
        public Joined(final List<Authentication> origins) {
            this.origins = origins;
        }

        @Override
        public Optional<AuthUser> user(final String user, final String pass) {
            return this.origins.stream()
                .map(auth -> auth.user(user, pass))
                .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
        }

        @Override
        public String toString() {
            return String.format(
                "%s([%s])",
                this.getClass().getSimpleName(),
                this.origins.stream().map(Object::toString).collect(Collectors.joining(","))
            );
        }
    }
}
