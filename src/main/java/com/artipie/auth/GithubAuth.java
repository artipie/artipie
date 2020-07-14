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
package com.artipie.auth;

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
     * @param github Github username resolver
     */
    GithubAuth(final Function<String, String> github) {
        this.github = github;
    }

    @Override
    public Optional<String> user(final String username, final String password) {
        Optional<String> result = Optional.empty();
        final Matcher matcher = GithubAuth.PTN_NAME.matcher(username);
        if (matcher.matches()) {
            final String login = this.github.apply(password).toLowerCase(Locale.US);
            if (
                Objects.equals(login, matcher.group(1).toLowerCase(Locale.US))
            ) {
                result = Optional.of(login);
            }
        }
        return result;
    }
}
