/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Authentication scheme such as Basic, Bearer etc.
 *
 * @since 0.17
 */
@SuppressWarnings({"PMD.ProhibitPublicStaticMethods",
    "PMD.ConstructorOnlyInitializesOrCallOtherConstructors"})
public interface AuthScheme {

    /**
     * Absent auth scheme that authenticates any request as "anonymous" user.
     */
    AuthScheme NONE = (hdrs, line)  -> CompletableFuture.completedFuture(
        new Result(AuthStatus.NO_CREDENTIALS, AuthUser.ANONYMOUS, null) {
            @Override
            public String challenge() {
                throw new UnsupportedOperationException();
            }
        }
    );

    /**
     * Authenticate HTTP request by its headers and request line.
     *
     * @param headers Request headers.
     * @param line Request line.
     * @return Authentication result.
     */
    CompletionStage<Result> authenticate(Iterable<Map.Entry<String, String>> headers, String line);

    /**
     * Authenticate HTTP request by its headers.
     *
     * @param headers Request headers.
     * @return Authentication result.
     */
    default CompletionStage<Result> authenticate(final Iterable<Map.Entry<String, String>> headers) {
        return this.authenticate(headers, "");
    }

    /**
     * Authentication status.
     */
    enum AuthStatus {
        /**
         * Successful authentication.
         */
        AUTHENTICATED,
        /**
         * Failed authentication.
         */
        FAILED,
        /**
         * A request doesn't contain credentials.
         */
        NO_CREDENTIALS
    }

    /**
     * Build authentication result.
     *
     * @param user Result's user
     * @param challenge Challenge value
     * @return Result
     */
    static Result result(final AuthUser user, final String challenge) {
        Objects.requireNonNull(user, "User must not be null!");
        final AuthStatus status = user.isAnonymous()
            ? AuthStatus.NO_CREDENTIALS
            : AuthStatus.AUTHENTICATED;
        return new Result(status, user, challenge);
    }

    /**
     * Build authentication result.
     *
     * @param user Result's user
     * @param challenge Challenge value
     * @return Result
     */
    static Result result(final Optional<AuthUser> user, final String challenge) {
        return user
            .map(
                authUser -> {
                    final AuthStatus status = authUser.isAnonymous()
                        ? AuthStatus.NO_CREDENTIALS
                        : AuthStatus.AUTHENTICATED;
                    return new Result(status, authUser, challenge);
                }).orElseGet(() -> new Result(AuthStatus.FAILED, null, challenge));
    }

    /**
     * HTTP request authentication result.
     *
     * @since 0.17
     */
    class Result {

        private final AuthStatus status;

        private final AuthUser user;

        private final String challenge;

        private Result(final AuthStatus status, final AuthUser user, final String challenge) {
            assert (status != AuthStatus.FAILED) == (user != null);
            this.status = status;
            this.user = user;
            this.challenge = challenge;
        }

        /**
         * Gets authentication status.
         *
         * @return AuthenticationStatus.
         */
        public AuthStatus status() {
            return this.status;
        }

        /**
         * Authenticated user.
         *
         * @return Authenticated user, empty if not authenticated.
         */
        public AuthUser user() {
            return this.user;
        }

        /**
         * Get authentication challenge that is provided in response WWW-Authenticate header value.
         *
         * @return Authentication challenge for client.
         */
        public String challenge() {
            return this.challenge;
        }

        @Override
        public String toString() {
            final String usr = this.status == AuthStatus.FAILED ? "" :
                ", user=" + this.user.name();
            return String.format("Result: [status=%s%s]", this.status, usr);
        }
    }

    /**
     * Fake implementation of {@link AuthScheme}.
     * @since 0.17.5
     */
    final class Fake implements AuthScheme {

        /**
         * Fake challange constant.
         */
        public static final String FAKE_CHLLNG = "fake";

        /**
         * Optional of User.
         */
        private final Optional<AuthUser> usr;

        /**
         * Challenge.
         */
        private final String chllng;

        /**
         * Ctor.
         * @param usr User
         * @param chllng Challenge
         */
        public Fake(final Optional<AuthUser> usr, final String chllng) {
            this.usr = usr;
            this.chllng = chllng;
        }

        /**
         * Ctor.
         * @param name User name
         */
        public Fake(final String name) {
            this(Optional.of(new AuthUser(name)), Fake.FAKE_CHLLNG);
        }

        /**
         * Ctor.
         */
        public Fake() {
            this(Optional.empty(), Fake.FAKE_CHLLNG);
        }

        @Override
        public CompletionStage<Result> authenticate(
            final Iterable<Map.Entry<String, String>> headers,
            final String line
        ) {
            return CompletableFuture.completedFuture(
                AuthScheme.result(this.usr, this.chllng)
            );
        }
    }
}
