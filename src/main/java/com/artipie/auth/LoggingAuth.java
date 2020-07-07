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
import com.jcabi.log.Logger;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Loggin implementation of {@link LoggingAuth}.
 * @since 0.9
 */
public final class LoggingAuth implements Authentication {

    /**
     * Origin authentication.
     */
    private final Authentication origin;

    /**
     * Log level.
     */
    private final Level level;

    /**
     * Decorates {@link Authentication} with {@code INFO} logger.
     * @param origin Authentication
     */
    public LoggingAuth(final Authentication origin) {
        this(origin, Level.INFO);
    }

    /**
     * Decorates {@link Authentication} with logger.
     * @param origin Origin auth
     * @param level Log level
     */
    public LoggingAuth(final Authentication origin, final Level level) {
        this.origin = origin;
        this.level = level;
    }

    @Override
    public Optional<String> user(final String username, final String password) {
        final Optional<String> res = this.origin.user(username, password);
        if (res.isEmpty()) {
            Logger.log(this.level, this.origin, "Failed to authenticate '%s' user", username);
        } else {
            Logger.log(this.level, this.origin, "Successfully authenticated '%s' user", username);
        }
        return res;
    }
}

