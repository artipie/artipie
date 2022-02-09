/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.ArtipieException;
import com.artipie.http.auth.Authentication;
import com.jcabi.github.RtGithub;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub authentication uses username prefixed by provider name {@code github.com}
 * and personal access token as a password.
 * See <a href="https://developer.github.com/v3/oauth_authorizations/">GitHub docs</a>
 * for details.
 * @implNote This implementation is not case sensitive.
 * @since 0.10
 */
public final class GithubAuth implements Authentication {

    /**
     * Username pattern, starts with provider name {@code github.com}, slash,
     * and GitHub username, e.g. {@code github.com/octocat}.
     */
    private static final Pattern PTN_NAME = Pattern.compile("^github\\.com/(.+)$");

    /**
     * Github username resolver by personal access token.
     */
    private final Function<String, String> github;

    /**
     * New GitHub authentication.
     * @checkstyle ReturnCountCheck (10 lines)
     */
    public GithubAuth() {
        this(
            token -> {
                try {
                    return new RtGithub(token).users().self().login();
                } catch (final IOException unauthorized) {
                    return "";
                }
            }
        );
    }

    /**
     * Primary constructor.
     * @param github Resolves GitHub token to username
     */
    GithubAuth(final Function<String, String> github) {
        this.github = github;
    }

    @Override
    public Optional<Authentication.User> user(final String username, final String password) {
        Optional<Authentication.User> result = Optional.empty();
        final Matcher matcher = GithubAuth.PTN_NAME.matcher(username);
        if (matcher.matches()) {
            try {
                final String login = this.github.apply(password).toLowerCase(Locale.US);
                if (
                    Objects.equals(login, matcher.group(1).toLowerCase(Locale.US))
                ) {
                    result = Optional.of(new Authentication.User(login));
                }
            } catch (final AssertionError error) {
                if (error.getMessage() == null
                    || !error.getMessage().contains("401 Unauthorized")) {
                    throw new ArtipieException(error);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
