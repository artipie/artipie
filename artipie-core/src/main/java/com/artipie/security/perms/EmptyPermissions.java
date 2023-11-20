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
 * Empty permissions' collection does not allow to add any elements and
 * does not imply any permission. To be used is cases when user or group have
 * none permissions declared.
 * @since 1.2
 */
public final class EmptyPermissions extends PermissionCollection {

    /**
     * Class instance.
     */
    public static final PermissionCollection INSTANCE = new EmptyPermissions();

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -8546496571451236952L;

    /**
     * Ctor.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    private EmptyPermissions() {
        this.setReadOnly();
    }

    @Override
    public void add(final Permission permission) {
        throw new NotImplementedException("This method is not implemented for EmptyPermissions");
    }

    @Override
    public boolean implies(final Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        return Collections.emptyEnumeration();
    }
}
