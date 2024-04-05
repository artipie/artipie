/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.security.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Operation controller for slice. The class is meant to check
 * if required permission is granted for user.
 * <p/>
 * Instances of this class are created in the adapter with users' policies and required
 * permission for the adapter's operation.
 */
public final class OperationControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationControl.class);

    /**
     * Security policy.
     */
    private final Policy<?> policy;

    /**
     * Required permissions (at least one should be allowed).
     */
    private final Collection<Permission> perms;

    /**
     * Ctor.
     * @param policy Security policy
     * @param perm Required permission
     */
    public OperationControl(final Policy<?> policy, final Permission perm) {
        this(policy, Collections.singleton(perm));
    }

    /**
     * Ctor.
     * @param policy Security policy
     * @param perms Required permissions (at least one should be allowed)
     */
    public OperationControl(final Policy<?> policy, final Permission... perms) {
        this(policy, List.of(perms));
    }

    /**
     * Ctor.
     * @param policy Security policy
     * @param perms Required permissions (at least one should be allowed)
     */
    public OperationControl(final Policy<?> policy, final Collection<Permission> perms) {
        this.policy = policy;
        this.perms = perms;
    }

    /**
     * Check if user is authorized to perform an action.
     * @param user User name
     * @return True if authorized
     */
    public boolean allowed(final AuthUser user) {
        final boolean res = perms.stream()
            .anyMatch(perm -> policy.getPermissions(user).implies(perm));
        LOGGER.debug(
            "Authorization operation: [permission={}, user={}, result={}}]",
            this.perms, user.name(), res ? "allowed" : "NOT allowed"
        );
        return res;
    }
}
