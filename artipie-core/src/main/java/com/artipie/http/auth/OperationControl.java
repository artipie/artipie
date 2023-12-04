/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.security.policy.Policy;
import com.jcabi.log.Logger;
import java.security.Permission;

/**
 * Operation controller for slice. The class is meant to check
 * if required permission is granted for user.
 * <p/>
 * Instances of this class are created in the adapter with users' policies and required
 * permission for the adapter's operation.
 * @since 1.2
 * @checkstyle StringLiteralsConcatenationCheck (500 lines)
 * @checkstyle AvoidInlineConditionalsCheck (500 lines)
 */
public final class OperationControl {

    /**
     * Security policy.
     */
    private final Policy<?> policy;

    /**
     * Required permission.
     */
    private final Permission perm;

    /**
     * Ctor.
     * @param policy Security policy
     * @param perm Required permission
     */
    public OperationControl(final Policy<?> policy, final Permission perm) {
        this.policy = policy;
        this.perm = perm;
    }

    /**
     * Check if user is authorized to perform an action.
     * @param user User name
     * @return True if authorized
     */
    public boolean allowed(final AuthUser user) {
        final boolean res = this.policy.getPermissions(user).implies(this.perm);
        Logger.debug(
            "security",
            "Authorization operation: [permission=%s, user=%s, result=%s]",
            this.perm, user.name(), res ? "allowed" : "NOT allowed"
        );
        return res;
    }
}
