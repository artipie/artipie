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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Authentication based on environment variables.
 * @since 0.3
 */
public final class AuthFromEnv implements Authentication {

    /**
     * Environment name for user.
     */
    public static final String ENV_NAME = "ARTIPIE_USER_NAME";

    /**
     * Environment name for password.
     */
    private static final String ENV_PASS = "ARTIPIE_USER_PASS";

    /**
     * Environment variables.
     */
    private final Map<String, String> env;

    /**
     * Default ctor with system environment.
     */
    public AuthFromEnv() {
        this(System.getenv());
    }

    /**
     * Primary ctor.
     * @param env Environment
     */
    public AuthFromEnv(final Map<String, String> env) {
        this.env = env;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Optional<String> user(final String username, final String password) {
        final Optional<String> result;
        // @checkstyle LineLengthCheck (5 lines)
        if (Objects.equals(Objects.requireNonNull(username), this.env.get(AuthFromEnv.ENV_NAME))
            && Objects.equals(Objects.requireNonNull(password), this.env.get(AuthFromEnv.ENV_PASS))) {
            result = Optional.of(username);
        } else {
            result = Optional.of("anonymous");
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
