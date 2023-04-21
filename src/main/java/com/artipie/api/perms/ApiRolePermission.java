/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.Action;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Permissions to manage roles.
 * @since 0.30
 */
public final class ApiRolePermission extends RestApiPermission {

    /**
     * Permission name.
     */
    static final String NAME = "api_role_permissions";

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -6000976571451236541L;

    /**
     * Ctor.
     * @param action Action
     */
    public ApiRolePermission(final RoleAction action) {
        super(ApiRolePermission.NAME, action.mask, new RoleActionList());
    }

    /**
     * Ctor.
     * @param actions Actions set
     */
    public ApiRolePermission(final Set<String> actions) {
        super(
            ApiRolePermission.NAME,
            RestApiPermission.maskFromActions(actions, new RoleActionList()),
            new RoleActionList()
        );
    }

    /**
     * Alias actions.
     * @since 0.29
     * @checkstyle JavadocVariableCheck (20 lines)
     * @checkstyle MagicNumberCheck (20 lines)
     */
    public enum RoleAction implements Action {
        READ(0x4),
        CREATE(0x2),
        UPDATE(0x1),
        MOVE(0x10),
        DELETE(0x8),
        ENABLE(0x20),
        ALL(0x4 | 0x2 | 0x8 | 0x10 | 0x1 | 0x20);

        /**
         * Action mask.
         */
        private final int mask;

        /**
         * Ctor.
         * @param mask Mask int
         */
        RoleAction(final int mask) {
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
    static final class RoleActionList extends ApiActions {

        /**
         * Ctor.
         */
        RoleActionList() {
            super(RoleAction.values());
        }

        @Override
        public Action all() {
            return RoleAction.ALL;
        }
    }
}
