/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.security.PermissionCollection;

/**
 * Permission factory to create permissions.
 * @param <T> Permission collection implementation
 * @since 1.2
 */
public interface PermissionFactory<T extends PermissionCollection> {

    /**
     * Create permissions collection.
     * @param config Configuration
     * @return Permission collection
     */
    T newPermissions(PermissionConfig config);
}
