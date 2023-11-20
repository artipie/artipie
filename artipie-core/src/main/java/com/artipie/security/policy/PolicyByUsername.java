/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.artipie.http.auth.AuthUser;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.perms.FreePermissions;
import java.security.PermissionCollection;

/**
 * Policy implementation for test: returns {@link FreePermissions} for
 * given name and {@link EmptyPermissions} for any other user.
 * @since 1.2
 */
public final class PolicyByUsername implements Policy<PermissionCollection> {

    /**
     * Username.
     */
    private final String name;

    /**
     * Ctor.
     * @param name Username
     */
    public PolicyByUsername(final String name) {
        this.name = name;
    }

    @Override
    public PermissionCollection getPermissions(final AuthUser user) {
        final PermissionCollection res;
        if (this.name.equals(user.name())) {
            res = new FreePermissions();
        } else {
            res = EmptyPermissions.INSTANCE;
        }
        return res;
    }
}
