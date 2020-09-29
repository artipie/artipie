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

package com.artipie;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.jcabi.log.Logger;
import java.util.logging.Level;

/**
 * Permissions decorator with logging.
 * @since 0.9
 */
public final class LoggingPermissions implements Permissions {

    /**
     * Origin permissions.
     */
    private final Permissions origin;

    /**
     * Log level.
     */
    private final Level level;

    /**
     * Decorates {@link Permissions} with info logging.
     * @param origin Permissions
     */
    public LoggingPermissions(final Permissions origin) {
        this(origin, Level.INFO);
    }

    /**
     * Decorates {@link Permissions} with logging.
     * @param origin Permissions
     * @param level Log level
     */
    public LoggingPermissions(final Permissions origin, final Level level) {
        this.origin = origin;
        this.level = level;
    }

    @Override
    public boolean allowed(final Authentication.User user, final String action) {
        final boolean res = this.origin.allowed(user, action);
        if (res) {
            Logger.log(
                this.level, this.origin, "Operation '%s' allowed for '%s'", action, user.name()
            );
        } else {
            Logger.log(
                this.level, this.origin, "Operation '%s' denied for '%s'", action, user.name()
            );
        }
        return res;
    }
}
