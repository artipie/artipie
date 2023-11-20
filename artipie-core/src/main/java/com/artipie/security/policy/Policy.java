/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.artipie.http.auth.AuthUser;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.FreePermissions;
import java.security.PermissionCollection;

/**
 * Security policy.
 *
 * @param <P> Implementation of {@link PermissionCollection}
 * @since 1.2
 */
public interface Policy<P extends PermissionCollection> {

    /**
     * Free policy for any user returns {@link FreePermissions} which implies any permission.
     */
    Policy<PermissionCollection> FREE = user -> new FreePermissions();

    /**
     * Get collection of permissions {@link PermissionCollection} for user by username.
     * <p>
     * Each user can have permissions of various types, for example:
     * list of {@link AdapterBasicPermission} for adapter with basic permissions and
     * another permissions' implementation for docker adapter.
     *
     * @param user User
     * @return Set of {@link PermissionCollection}
     */
    P getPermissions(AuthUser user);

}
