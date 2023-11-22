/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Collections;

/**
 * User provides its individual permission collection and
 * groups.
 * @since 1.2
 */
public interface User {

    /**
     * Empty user with no permissions and no roles.
     */
    User EMPTY = new User() {
        @Override
        public Collection<String> roles() {
            return Collections.emptyList();
        }

        @Override
        public PermissionCollection perms() {
            return EmptyPermissions.INSTANCE;
        }
    };

    /**
     * Returns user groups.
     * @return Collection of the groups
     */
    Collection<String> roles();

    /**
     * Returns user's individual permissions.
     * @return Individual permissions collection
     */
    PermissionCollection perms();

}
