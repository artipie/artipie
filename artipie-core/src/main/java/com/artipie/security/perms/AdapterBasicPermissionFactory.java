/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

/**
 * Factory for {@link AdapterBasicPermission}.
 * @since 1.2
 */
@ArtipiePermissionFactory("adapter_basic_permissions")
public final class AdapterBasicPermissionFactory implements
    PermissionFactory<AdapterBasicPermission.AdapterBasicPermissionCollection> {

    @Override
    public AdapterBasicPermission.AdapterBasicPermissionCollection newPermissions(
        final PermissionConfig config
    ) {
        final AdapterBasicPermission.AdapterBasicPermissionCollection res =
            new AdapterBasicPermission.AdapterBasicPermissionCollection();
        for (final String name : config.keys()) {
            res.add(new AdapterBasicPermission(name, config.sequence(name)));
        }
        return res;
    }

}
