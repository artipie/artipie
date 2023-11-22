/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.Action;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Permissions to manage users.
 * @since 0.30
 */
public final class ApiUserPermission extends RestApiPermission {

    /**
     * Permission name.
     */
    static final String NAME = "api_user_permissions";

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -1910976021451236971L;

    /**
     * User action list.
     */
    private static final UserActionList ACTION_LIST = new UserActionList();

    /**
     * Ctor.
     * @param action Action
     */
    public ApiUserPermission(final UserAction action) {
        super(ApiUserPermission.NAME, action.mask, ApiUserPermission.ACTION_LIST);
    }

    /**
     * Ctor.
     * @param actions Actions set
     */
    public ApiUserPermission(final Set<String> actions) {
        super(
            ApiUserPermission.NAME,
            RestApiPermission.maskFromActions(actions, ApiUserPermission.ACTION_LIST),
            ApiUserPermission.ACTION_LIST
        );
    }

    @Override
    public ApiUserPermissionCollection newPermissionCollection() {
        return new ApiUserPermissionCollection();
    }

    /**
     * Collection of the user permissions.
     * @since 0.30
     */
    static final class ApiUserPermissionCollection extends RestApiPermissionCollection {

        /**
         * Required serial.
         */
        private static final long serialVersionUID = -3000962571451212361L;

        /**
         * Ctor.
         */
        ApiUserPermissionCollection() {
            super(ApiUserPermission.class);
        }
    }

    /**
     * User actions.
     * @since 0.29
     * @checkstyle JavadocVariableCheck (20 lines)
     * @checkstyle MagicNumberCheck (20 lines)
     */
    public enum UserAction implements Action {
        READ(0x4),
        CREATE(0x2),
        UPDATE(0x1),
        DELETE(0x8),
        ENABLE(0x10),
        CHANGE_PASSWORD(0x20),
        ALL(0x4 | 0x2 | 0x8 | 0x20 | 0x1 | 0x10);

        /**
         * Action mask.
         */
        private final int mask;

        /**
         * Ctor.
         * @param mask Mask int
         */
        UserAction(final int mask) {
            this.mask = mask;
        }

        @Override
        public Set<String> names() {
            return Collections.singleton(this.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public int mask() {
            return this.mask;
        }
    }

    /**
     * Manage user and roles actions list.
     * @since 0.30
     */
    static final class UserActionList extends ApiActions {

        /**
         * Ctor.
         */
        UserActionList() {
            super(UserAction.values());
        }

        @Override
        public Action all() {
            return UserAction.ALL;
        }
    }
}
