/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import io.vertx.core.impl.ConcurrentHashSet;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Base class for rest api permissions.
 * @since 0.30
 */
public abstract class RestApiPermission extends Permission {

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -2910976571451236971L;

    /**
     * Wildcard symbol.
     */
    private static final String WILDCARD = "*";

    /**
     * Canonical action representation. Is initialized once on request
     * by {@link AdapterBasicPermission#getActions()} method.
     */
    private String actions;

    /**
     * Action mask.
     */
    private final transient int mask;

    /**
     * Actions type.
     */
    private final transient ApiActions type;

    /**
     * True, is any action is allowed.
     */
    private final transient boolean any;

    /**
     * Ctor.
     * @param name Permission implementation name
     * @param mask Permission action mask
     * @param type Actions type
     */
    protected RestApiPermission(final String name, final int mask, final ApiActions type) {
        super(name);
        this.mask = mask;
        this.type = type;
        this.any = this.mask == type.all().mask();
    }

    @Override
    public final boolean implies(final Permission permission) {
        final boolean res;
        if (permission != null && permission.getClass() == this.getClass()) {
            final RestApiPermission that = (RestApiPermission) permission;
            res = (this.mask & that.mask) == that.mask;
        } else {
            res = false;
        }
        return res;
    }

    @Override
    public final boolean equals(final Object obj) {
        final boolean res;
        if (obj == this) {
            res = true;
        } else if (obj != null && obj.getClass() == this.getClass()) {
            final RestApiPermission that = (RestApiPermission) obj;
            res = that.mask == this.mask && Objects.equals(that.getName(), this.getName());
        } else {
            res = false;
        }
        return res;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.getName(), this.mask);
    }

    @Override
    public final String getActions() {
        if (this.actions == null) {
            final StringJoiner joiner = new StringJoiner(",");
            for (final Action item : this.type.list()) {
                if ((this.mask & item.mask()) == item.mask()) {
                    joiner.add(item.names().iterator().next());
                }
            }
            this.actions = joiner.toString();
        }
        return this.actions;
    }

    /**
     * Calculate mask from action.
     * @param actions The set of actions
     * @param type Actions type
     * @return Integer mask
     */
    protected static int maskFromActions(final Collection<String> actions, final ApiActions type) {
        int res = Action.NONE.mask();
        if (actions.isEmpty() || actions.size() == 1 && actions.contains("")) {
            res = Action.NONE.mask();
        } else if (actions.contains(RestApiPermission.WILDCARD)) {
            res = type.all().mask();
        } else {
            for (final String item : actions) {
                res |= type.maskByAction().apply(item);
            }
        }
        return res;
    }

    /**
     * Collection of {@link RestApiPermission} objects, can contain any child of
     * {@link RestApiPermission}. This collection is homogeneous, type class is stored in
     * {@link RestApiPermissionCollection#clazz} field.
     * <p>
     * If any action is allowed for added permission, then flag
     * {@link RestApiPermissionCollection#any} is set to true and any other permissions are cleared
     * from the set. Note, that according to
     * the permissions model, this collection actually contains only one permission per user.
     * @since 1.2
     */
    abstract static class RestApiPermissionCollection extends PermissionCollection
        implements java.io.Serializable {

        /**
         * Required serial.
         */
        private static final long serialVersionUID = 5843036924729092120L;

        /**
         * All permission objects in collection must be of the same type.
         * Not serialized.
         */
        private final transient Set<Permission> perms;

        /**
         * This is set to {@code true} if this collection
         * contains a permission with action all.
         */
        private boolean any;

        /**
         * Permission class type.
         */
        private final Class<?> clazz;

        /**
         * Ctor.
         * @param clazz Collection class type
         */
        RestApiPermissionCollection(final Class<?> clazz) {
            this.clazz = clazz;
            this.perms = new ConcurrentHashSet<>(1);
        }

        @Override
        public void add(final Permission permission) {
            if (permission.getClass() != this.clazz) {
                throw new IllegalArgumentException(
                    String.format("Invalid permission type %s", permission.getClass())
                );
            }
            if (isReadOnly()) {
                throw new SecurityException(
                    "Attempt to add a Permission to a readonly PermissionCollection"
                );
            }
            if (!this.any) {
                final RestApiPermission item = (RestApiPermission) permission;
                if (item.any) {
                    this.any = true;
                    this.perms.clear();
                }
                this.perms.add(item);
            }
        }

        @Override
        public boolean implies(final Permission permission) {
            boolean res = false;
            if (permission.getClass() == this.clazz) {
                if (this.any) {
                    res = true;
                } else {
                    for (final Permission item : this.perms) {
                        if (item.implies(permission)) {
                            res = true;
                            break;
                        }
                    }
                }
            }
            return res;
        }

        @Override
        public Enumeration<Permission> elements() {
            return Collections.enumeration(this.perms);
        }
    }
}
