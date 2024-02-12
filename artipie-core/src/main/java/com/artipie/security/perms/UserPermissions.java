/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.io.Serial;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of {@link PermissionCollection} for user. It takes into account
 * user individual permissions and his roles. The roles and user permissions
 * are represented by {@link Supplier} interface in order they can actually be
 * addressed on demand only. The same goes for permissions for role, they are represented by
 * {@link Function} interface and are addressed only on demand.
 * <p/>
 * The implementations of the {@link Supplier} and {@link Function} are user's choose, it can
 * read permissions from file on each call or use some complex caches inside.
 * <p/>
 * Method {@link UserPermissions#implies(Permission)} implementation note:
 * <p/>
 * first, we check if the permission is implied according to the {@link UserPermissions#lastRole}
 * reference calling {@link UserPermissions#checkReference(String, Permission)} method.
 * Synchronization is not required here as
 * <p>
 * 1) we do not change the value of {@link UserPermissions#lastRole} field
 * </p>
 * <p>
 * 2) it does not matter if the {@link UserPermissions#lastRole} value was changed in the synchronized
 *   section by other thread if we get positive result.
 * </p>
 * <p/>
 * second, if we do not get the positive result, we enter synchronized section, get the value
 * from {@link UserPermissions#lastRole} and check it again if the value was changed. Then, if the
 * result is still negative, we perform the whole check by the user's personal permissions and all
 * the groups.
 *
 * @since 1.2
 */
public final class UserPermissions extends PermissionCollection {

    @Serial
    private static final long serialVersionUID = -7546496571951236695L;

    /**
     * Lock object.
     */
    private final transient Object lock;

    /**
     * Role permissions.
     */
    private final transient Function<String, PermissionCollection> rperms;

    /**
     * User with his roles and individual permissions.
     */
    private final transient Supplier<User> user;

    /**
     * The name of the group, which implied the permission in the previous
     * {@link UserPermissions#implies(Permission)} method call. Empty if
     * user permissions implied the permission.
     */
    private final AtomicReference<String> lastRole;

    /**
     * Ctor.
     * @param user User individual permissions and roles
     * @param rperms Role permissions
     */
    public UserPermissions(
        final Supplier<User> user,
        final Function<String, PermissionCollection> rperms
    ) {
        this.rperms = rperms;
        this.user = user;
        this.lastRole = new AtomicReference<>();
        this.lock = new Object();
    }

    @Override
    public void add(final Permission permission) {
        this.user.get().perms().add(permission);
    }

    @Override
    @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts", "PMD.CognitiveComplexity"})
    public boolean implies(final Permission permission) {
        final String first = this.lastRole.get();
        boolean res = this.checkReference(first, permission);
        if (!res) {
            synchronized (this.lock) {
                final String second = this.lastRole.get();
                if (!Objects.equals(first, second)) {
                    res = this.checkReference(second, permission);
                }
                if (!res) {
                    if (second != null) {
                        res = this.user.get().perms().implies(permission);
                    }
                    if (res) {
                        this.lastRole.set(null);
                    } else {
                        for (final String role : this.user.get().roles()) {
                            if (!role.equals(second)
                                && this.rperms.apply(role).implies(permission)) {
                                res = true;
                                this.lastRole.set(role);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    @Override
    public Enumeration<Permission> elements() {
        return this.user.get().perms().elements();
    }

    /**
     * Check the permission according to the given reference (group or individual perms).
     * @param ref The reference for the check
     * @param permission The permission to check
     * @return The result, true if according to the last ref permission is implied
     */
    private boolean checkReference(final String ref, final Permission permission) {
        if (ref == null) {
            return this.user.get().perms().implies(permission);
        } else {
            return this.rperms.apply(ref).implies(permission);
        }
    }
}
