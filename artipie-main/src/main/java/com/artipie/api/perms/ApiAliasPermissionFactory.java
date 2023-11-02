/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.ArtipiePermissionFactory;
import com.artipie.security.perms.PermissionConfig;
import com.artipie.security.perms.PermissionFactory;

/**
 * Factory for {@link ApiAliasPermission}.
 * @since 0.30
 */
@ArtipiePermissionFactory(ApiAliasPermission.NAME)
public final class ApiAliasPermissionFactory implements
    PermissionFactory<RestApiPermission.RestApiPermissionCollection> {

    @Override
    public RestApiPermission.RestApiPermissionCollection newPermissions(
        final PermissionConfig cfg
    ) {
        final ApiAliasPermission perm = new ApiAliasPermission(cfg.keys());
        final RestApiPermission.RestApiPermissionCollection collection =
            perm.newPermissionCollection();
        collection.add(perm);
        return collection;
    }
}
