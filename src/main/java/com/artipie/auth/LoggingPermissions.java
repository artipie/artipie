/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.auth;

import com.artipie.http.auth.AuthUser;
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
    public boolean allowed(final AuthUser user, final String action) {
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
