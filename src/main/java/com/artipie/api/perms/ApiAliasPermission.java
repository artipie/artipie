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
 * Permissions to manage storage aliases.
 * @since 0.30
 */
public final class ApiAliasPermission extends RestApiPermission {

    /**
     * Permission name.
     */
    static final String NAME = "api_storage_alias_permissions";

    /**
     * Required serial.
     */
    private static final long serialVersionUID = -2910962571451212361L;

    /**
     * Ctor.
     * @param action Action
     */
    public ApiAliasPermission(final AliasAction action) {
        super(ApiAliasPermission.NAME, action.mask, new AliasActionList());
    }

    /**
     * Ctor.
     * @param actions Actions set
     */
    public ApiAliasPermission(final Set<String> actions) {
        super(
            ApiAliasPermission.NAME,
            ApiAliasPermission.maskFromActions(actions, new AliasActionList()),
            new AliasActionList()
        );
    }

    /**
     * Alias actions.
     * @since 0.29
     * @checkstyle JavadocVariableCheck (20 lines)
     * @checkstyle MagicNumberCheck (20 lines)
     */
    public enum AliasAction implements Action {
        READ(0x4),
        CREATE(0x2),
        DELETE(0x8),
        ALL(0x4 | 0x2 | 0x8);

        /**
         * Action mask.
         */
        private final int mask;

        /**
         * Ctor.
         * @param mask Mask int
         */
        AliasAction(final int mask) {
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
     * Manage aliases actions list.
     * @since 0.30
     */
    static final class AliasActionList extends ApiActions {

        /**
         * Ctor.
         */
        AliasActionList() {
            super(AliasAction.values());
        }

        @Override
        public Action all() {
            return AliasAction.ALL;
        }

    }
}
