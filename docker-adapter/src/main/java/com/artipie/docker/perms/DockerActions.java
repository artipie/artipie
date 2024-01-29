/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.artipie.security.perms.Action;
import java.util.Collections;
import java.util.Set;

/**
 * Docker actions.
 * @since 0.18
 */
public enum DockerActions implements Action {

    PULL(0x4, "pull"),
    PUSH(0x2, "push"),
    OVERWRITE(0x10, "overwrite"),
    ALL(0x4 | 0x2 | 0x10, "*");

    /**
     * Action mask.
     */
    private final int mask;

    /**
     * Action name.
     */
    private final String name;

    /**
     * Ctor.
     * @param mask Action mask
     * @param name Action name
     */
    DockerActions(final int mask, final String name) {
        this.mask = mask;
        this.name = name;
    }

    @Override
    public Set<String> names() {
        return Collections.singleton(this.name);
    }

    @Override
    public int mask() {
        return this.mask;
    }

    /**
     * Get action int mask by name.
     * @param name The action name
     * @return The mask
     * @throws IllegalArgumentException is the action not valid
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static int maskByAction(final String name) {
        for (final Action item : values()) {
            if (item.names().contains(name)) {
                return item.mask();
            }
        }
        throw new IllegalArgumentException(
            String.format("Unknown permission action %s", name)
        );
    }
}
