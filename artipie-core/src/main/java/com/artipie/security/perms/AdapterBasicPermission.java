/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Artipie basic permission. This permission takes into account repository name and
 * the set of actions. Both parameters are required, repository name is composite in the
 * case of ORG layout {user_name}/{repo_name}.
 * Supported actions are: read, write, delete. Wildcard * is also supported and means,
 * that any actions is allowed.
 * This permission implies another permission if permissions names are equal and
 * this permission allows all actions from another permission.
 * @since 1.2
 */
public final class AdapterBasicPermission extends Permission {

    /**
     * Wildcard symbol.
     */
    static final String WILDCARD = "*";

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -2916496571451236071L;

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
     * Primary ctor.
     * @param name Perm name
     * @param mask Mask
     */
    private AdapterBasicPermission(final String name, final int mask) {
        super(name);
        this.mask = mask;
    }

    /**
     * Ctor.
     * @param repo Repository name
     * @param strings Actions set
     */
    public AdapterBasicPermission(final String repo, final String strings) {
        this(repo, Stream.of(strings.split(",")).collect(Collectors.toSet()));
    }

    /**
     * Ctor.
     * @param repo Repository name
     * @param action Action
     */
    public AdapterBasicPermission(final String repo, final Action action) {
        this(repo, action.mask());
    }

    /**
     * Ctor.
     * @param repo Repository name
     * @param strings Actions set
     */
    public AdapterBasicPermission(final String repo, final Collection<String> strings) {
        this(repo, AdapterBasicPermission.maskFromActions(strings));
    }

    @Override
    public boolean implies(final Permission permission) {
        final boolean res;
        if (permission instanceof AdapterBasicPermission) {
            final AdapterBasicPermission that = (AdapterBasicPermission) permission;
            res = (this.mask & that.mask) == that.mask && this.impliesIgnoreMask(that);
        } else {
            res = false;
        }
        return res;
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean res;
        if (obj == this) {
            res = true;
        } else if (obj instanceof AdapterBasicPermission) {
            final AdapterBasicPermission that = (AdapterBasicPermission) obj;
            res = that.mask == this.mask && Objects.equals(that.getName(), this.getName());
        } else {
            res = false;
        }
        return res;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String getActions() {
        if (this.actions == null) {
            final StringJoiner joiner = new StringJoiner(",");
            if ((this.mask & Action.Standard.READ.mask()) == Action.Standard.READ.mask()) {
                joiner.add(Action.Standard.READ.name().toLowerCase(Locale.ROOT));
            }
            if ((this.mask & Action.Standard.WRITE.mask()) == Action.Standard.WRITE.mask()) {
                joiner.add(Action.Standard.WRITE.name().toLowerCase(Locale.ROOT));
            }
            if ((this.mask & Action.Standard.DELETE.mask()) == Action.Standard.DELETE.mask()) {
                joiner.add(Action.Standard.DELETE.name().toLowerCase(Locale.ROOT));
            }
            this.actions = joiner.toString();
        }
        return this.actions;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new AdapterBasicPermissionCollection();
    }

    /**
     * Check if this action implies another action ignoring mask. That is true if
     * permissions names are equal or this permission has wildcard name.
     * @param perm Permission to check for imply
     * @return True when implies
     */
    private boolean impliesIgnoreMask(final AdapterBasicPermission perm) {
        final boolean res;
        if (this.getName().equals(AdapterBasicPermission.WILDCARD)) {
            res = true;
        } else {
            res = this.getName().equalsIgnoreCase(perm.getName());
        }
        return res;
    }

    /**
     * Calculate mask from action.
     * @param actions The set of actions
     * @return Integer mask
     */
    private static int maskFromActions(final Collection<String> actions) {
        int res = Action.NONE.mask();
        if (actions.isEmpty() || actions.size() == 1 && actions.contains("")) {
            res = Action.NONE.mask();
        } else if (actions.contains(AdapterBasicPermission.WILDCARD)) {
            res = Action.ALL.mask();
        } else {
            for (final String item : actions) {
                res |= Action.Standard.maskByAction(item);
            }
        }
        return res;
    }

    /**
     * Collection of {@link AdapterBasicPermission} objects.
     * @since 1.2
     */
    static final class AdapterBasicPermissionCollection extends PermissionCollection
        implements java.io.Serializable {

        /**
         * Required serial.
         */
        private static final long serialVersionUID = 5843017424729092155L;

        /**
         * Key is name, value is permission. All permission objects in
         * collection must be of the same type.
         * Not serialized; see serialization section at end of class.
         */
        private final transient ConcurrentHashMap<String, Permission> perms;

        /**
         * This is set to {@code true} if this AdapterBasicPermissionCollection
         * contains a AdapterBasicPermission with '*' as its permission name
         * and {@link Action#ALL} action.
         */
        private boolean any;

        /**
         * Create an empty BasicPermissionCollection object.
         * @checkstyle MagicNumberCheck (5 lines)
         */
        AdapterBasicPermissionCollection() {
            this.perms = new ConcurrentHashMap<>(5);
            this.any = false;
        }

        @Override
        public void add(final Permission permission) {
            if (this.isReadOnly()) {
                throw new SecurityException(
                    "attempt to add a Permission to a readonly PermissionCollection"
                );
            }
            if (permission instanceof AdapterBasicPermission) {
                this.perms.put(permission.getName(), permission);
                if (permission.getName().equals(AdapterBasicPermission.WILDCARD)
                    && ((AdapterBasicPermission) permission).mask == Action.ALL.mask()) {
                    this.any = true;
                }
            } else {
                throw new IllegalArgumentException(
                    String.format("Invalid permissions type %s", permission.getClass())
                );
            }
        }

        @Override
        public boolean implies(final Permission permission) {
            boolean res = false;
            if (permission instanceof AdapterBasicPermission) {
                if (this.any) {
                    res = true;
                } else {
                    //@checkstyle NestedIfDepthCheck (10 lines)
                    Permission existing = this.perms.get(permission.getName());
                    if (existing != null) {
                        res = existing.implies(permission);
                    }
                    if (!res) {
                        existing = this.perms.get(AdapterBasicPermission.WILDCARD);
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
            return this.perms.elements();
        }
    }
}
