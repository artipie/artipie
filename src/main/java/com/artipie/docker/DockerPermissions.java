/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.docker.http.Scope;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;

/**
 * Docker permissions adapter that translates Docker scope actions to Artipie actions.
 *
 * @since 0.15
 */
public final class DockerPermissions implements Permissions {

    /**
     * Origin permissions.
     */
    private final Permissions origin;

    /**
     * Ctor.
     *
     * @param origin Origin permissions.
     */
    public DockerPermissions(final Permissions origin) {
        this.origin = origin;
    }

    @Override
    public boolean allowed(final Authentication.User user, final String action) {
        final Action translated;
        switch (new Scope.FromString(action).action()) {
            case "pull":
            case "*":
                translated = Action.Standard.READ;
                break;
            case "push":
            case "overwrite":
                translated = Action.Standard.WRITE;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unexpected action: %s", action));
        }
        return new Permission.ByName(this.origin, translated).allowed(user);
    }
}
