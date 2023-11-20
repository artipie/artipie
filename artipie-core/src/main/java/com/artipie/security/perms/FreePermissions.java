/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Free permissions implies any permission.
 * @since 1.2
 */
public final class FreePermissions extends PermissionCollection {

    /**
     * Required serial.
     */
    private static final long serialVersionUID = 1346496579871236952L;

    @Override
    public void add(final Permission permission) {
        throw new NotImplementedException(
            "This permission collection does not support adding elements"
        );
    }

    @Override
    public boolean implies(final Permission permission) {
        return true;
    }

    @Override
    public Enumeration<Permission> elements() {
        return Collections.emptyEnumeration();
    }
}
