/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.artipie.docker.http.Scope;
import com.artipie.security.perms.Action;

import java.io.Serial;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permissions for docker registry categories.
 * @since 0.18
 */
public final class DockerRegistryPermission extends Permission {

    @Serial
    private static final long serialVersionUID = 3016435961451239611L;

    /**
     * Canonical action representation. Is initialized once on request
     * by {@link DockerRegistryPermission#getActions()} method.
     */
    private String actions;

    /**
     * Category mask.
     */
    private final transient int mask;

    /**
     * Constructs a permission with the specified name.
     *
     * @param name Name of the Permission object being created.
     * @param mask Categories mask
     */
    public DockerRegistryPermission(final String name, final int mask) {
        super(name);
        this.mask = mask;
    }

    /**
     * Constructs a permission with the specified name and scope.
     *
     * @param name Name of the Permission object being created.
     * @param scope Permission scope, see {@link Scope.Registry}
     */
    public DockerRegistryPermission(final String name, final Scope scope) {
        this(name, scope.action().mask());
    }

    /**
     * Constructs a permission with the specified name.
     *
     * @param name Name of the Permission object being created.
     * @param categories Categories list
     */
    public DockerRegistryPermission(final String name, final Collection<String> categories) {
        this(name, maskFromCategories(categories));
    }

    @Override
    public boolean implies(final Permission permission) {
        if (permission instanceof DockerRegistryPermission that) {
            return (this.mask & that.mask) == that.mask && this.impliesIgnoreMask(that);
        }
        return false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DockerRegistryPermission that) {
            return that.getName().equals(this.getName()) && that.mask == this.mask;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String getActions() {
        if (this.actions == null) {
            final StringJoiner joiner = new StringJoiner(",");
            if ((this.mask & RegistryCategory.BASE.mask()) == RegistryCategory.BASE.mask()) {
                joiner.add(RegistryCategory.BASE.name().toLowerCase(Locale.ROOT));
            }
            if ((this.mask & RegistryCategory.CATALOG.mask()) == RegistryCategory.CATALOG.mask()) {
                joiner.add(RegistryCategory.CATALOG.name().toLowerCase(Locale.ROOT));
            }
            this.actions = joiner.toString();
        }
        return this.actions;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new DockerRegistryPermissionCollection();
    }

    /**
     * Check if this action implies another action ignoring mask. That is true if
     * permissions names are equal or this permission has wildcard name.
     * @param perm Permission to check for imply
     * @return True when implies
     */
    private boolean impliesIgnoreMask(final DockerRegistryPermission perm) {
        return this.getName().equals(DockerRepositoryPermission.WILDCARD) ||
                this.getName().equalsIgnoreCase(perm.getName());
    }

    /**
     * Calculate mask from action.
     * @param categories The set of actions
     * @return Integer mask
     */
    private static int maskFromCategories(final Collection<String> categories) {
        int res = Action.NONE.mask();
        if (categories.isEmpty() || categories.size() == 1 && categories.contains("")) {
            res = Action.NONE.mask();
        } else if (categories.contains(DockerRepositoryPermission.WILDCARD)) {
            res = RegistryCategory.ANY.mask();
        } else {
            for (final String item : categories) {
                res |= RegistryCategory.maskByCategory(item);
            }
        }
        return res;
    }

    /**
     * Collection of {@link DockerRegistryPermission} objects.
     * @since 0.18
     */
    public static final class DockerRegistryPermissionCollection extends PermissionCollection
        implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = -2153247295984095455L;

        /**
         * Correction of the repository type permissions.
         * Key is `name`, value is permission. All permission objects in
         * collection must be of the same type.
         * Not serialized; see serialization section at end of class.
         */
        private final transient ConcurrentHashMap<String, Permission> collection;

        /**
         * This is set to {@code true} if this DockerRegistryPermissionCollection
         * contains a permission with '*' as its permission name,
         * {@link RegistryCategory#ANY} category.
         */
        private boolean any;

        /**
         * Create an empty object.
         */
        public DockerRegistryPermissionCollection() {
            this.collection = new ConcurrentHashMap<>(5);
            this.any = false;
        }

        @Override
        public void add(final Permission obj) {
            if (this.isReadOnly()) {
                throw new SecurityException(
                    "attempt to add a Permission to a readonly PermissionCollection"
                );
            }
            if (obj instanceof DockerRegistryPermission perm) {
                this.collection.put(perm.getName(), perm);
                if (DockerRepositoryPermission.WILDCARD.equals(perm.getName())
                    && RegistryCategory.ANY.mask() == perm.mask) {
                    this.any = true;
                }
            } else {
                throw new IllegalArgumentException(
                    String.format("Invalid permissions type %s", obj.getClass())
                );
            }
        }

        @Override
        public boolean implies(final Permission permission) {
            boolean res = false;
            if (permission instanceof DockerRegistryPermission) {
                if (this.any) {
                    res = true;
                } else {
                    Permission existing = this.collection.get(permission.getName());
                    if (existing != null) {
                        res = existing.implies(permission);
                    }
                    if (!res) {
                        existing = this.collection.get(DockerRepositoryPermission.WILDCARD);
                        if (existing != null) {
                            res = existing.implies(permission);
                        }
                    }
                }
            }
            return res;
        }

        @Override
        public Enumeration<Permission> elements() {
            return this.collection.elements();
        }
    }
}
