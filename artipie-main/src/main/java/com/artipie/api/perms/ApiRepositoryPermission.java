/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.Action;
import java.util.Collections;
import java.util.Set;

/**
 * Permissions to manage repository settings.
 * @since 0.30
 */
public final class ApiRepositoryPermission extends RestApiPermission {

    /**
     * Permission name.
     */
    static final String NAME = "api_repository_permissions";

    /**
     * Required serial.
     */
    private static final long serialVersionUID = 8910976571453906971L;

    /**
     * Repository actions list.
     */
    private static final RepositoryActionList ACTION_LIST = new RepositoryActionList();

    /**
     * Ctor.
     * @param action Action
     */
    public ApiRepositoryPermission(final RepositoryAction action) {
        super(ApiRepositoryPermission.NAME, action.mask, ApiRepositoryPermission.ACTION_LIST);
    }

    /**
     * Ctor.
     * @param actions Actions set
     */
    public ApiRepositoryPermission(final Set<String> actions) {
        super(
            ApiRepositoryPermission.NAME,
            RestApiPermission.maskFromActions(actions, ApiRepositoryPermission.ACTION_LIST),
            ApiRepositoryPermission.ACTION_LIST
        );
    }

    @Override
    public ApiRepositoryPermissionCollection newPermissionCollection() {
        return new ApiRepositoryPermissionCollection();
    }

    /**
     * Collection of the repository permissions.
     * @since 0.30
     */
    static final class ApiRepositoryPermissionCollection extends RestApiPermissionCollection {

        /**
         * Required serial.
         */
        private static final long serialVersionUID = -1010962571451212361L;

        /**
         * Ctor.
         */
        ApiRepositoryPermissionCollection() {
            super(ApiRepositoryPermission.class);
        }
    }

    /**
     * Repository actions.
     * @since 0.29
     */
    public enum RepositoryAction implements Action {
        READ(0x4),
        CREATE(0x2),
        UPDATE(0x1),
        MOVE(0x10),
        DELETE(0x8),
        ALL(0x4 | 0x2 | 0x8 | 0x10 | 0x1);

        /**
         * Action mask.
         */
        private final int mask;

        /**
         * Ctor.
         * @param mask Mask int
         */
        RepositoryAction(final int mask) {
            this.mask = mask;
        }

        @Override
        public Set<String> names() {
            return Collections.singleton(this.name().toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public int mask() {
            return this.mask;
        }
    }

    /**
     * Manage repository actions list.
     * @since 0.30
     */
    static final class RepositoryActionList extends ApiActions {

        /**
         * Ctor.
         */
        RepositoryActionList() {
            super(RepositoryAction.values());
        }

        @Override
        public Action all() {
            return RepositoryAction.ALL;
        }
    }
}
