/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer.test;

import com.artipie.http.auth.Authentication;

/**
 * Basic authentication for usage in tests. Alice is authenticated.
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
    public static final User BOB = new User("Bob", "123");

    /**
     * Ctor.
     */
    public TestAuthentication() {
        super(
            new Authentication.Single(
                TestAuthentication.ALICE.name(),
                TestAuthentication.ALICE.password()
            )
        );
    }

    /**
     * User with name and password.
     * @since 0.4
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
         * @param username Username
         * @param pwd Password
         */
        User(final String username, final String pwd) {
            this.username = username;
            this.pwd = pwd;
        }

        /**
         * Get username.
         * @return Username.
         */
        public String name() {
            return this.username;
        }

        /**
         * Get password.
         * @return Password.
         */
        public String password() {
            return this.pwd;
        }

        @Override
        public String toString() {
            return this.username;
        }
    }
}

