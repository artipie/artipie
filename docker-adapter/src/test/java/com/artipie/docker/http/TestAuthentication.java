/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic authentication for usage in tests aware of two users: Alice and Bob.
 *
 * @since 0.4
 */
public final class TestAuthentication extends Authentication.Wrap {

    /**
     * Example Alice user.
     */
    public static final User ALICE = new User("Alice", "OpenSesame");

    /**
     * Example Bob user.
     */
    public static final User BOB = new User("Bob", "iamgod");

    /**
     * Ctor.
     */
    protected TestAuthentication() {
        super(
            new Authentication.Joined(
                Stream.of(TestAuthentication.ALICE, TestAuthentication.BOB)
                    .map(user -> new Authentication.Single(user.name(), user.password()))
                    .collect(Collectors.toList())
            )
        );
    }

    /**
     * User with name and password.
     *
     * @since 0.5
     */
    public static final class User {

        /**
         * Username.
         */
        private final String username;

        /**
         * Password.
         */
        private final String pwd;

        /**
         * Ctor.
         *
         * @param username Username.
         * @param pwd Password.
         */
        User(final String username, final String pwd) {
            this.username = username;
            this.pwd = pwd;
        }

        /**
         * Get username.
         *
         * @return Username.
         */
        public String name() {
            return this.username;
        }

        /**
         * Get password.
         *
         * @return Password.
         */
        public String password() {
            return this.pwd;
        }

        /**
         * Create basic authentication headers.
         *
         * @return Headers.
         */
        public Headers headers() {
            return new Headers.From(
                new Authorization.Basic(this.name(), this.password())
            );
        }

        @Override
        public String toString() {
            return this.username;
        }
    }
}
