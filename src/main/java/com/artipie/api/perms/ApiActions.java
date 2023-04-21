/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.perms;

import com.artipie.security.perms.Action;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Api actions.
 * @since 0.30
 */
@SuppressWarnings({"PMD.ArrayIsStoredDirectly", "PMD.UseVarargs"})
public abstract class ApiActions {

    /**
     * Action values list.
     */
    private final Action[] values;

    /**
     * Ctor.
     * @param values Action values list
     */
    protected ApiActions(final Action[] values) {
        this.values = values;
    }

    /**
     * Returns action, that represents all (action tha allows any action) actions.
     * @return Action all
     */
    abstract Action all();

    /**
     * All supported actions list.
     * @return All supported actions
     */
    Collection<Action> list() {
        return Stream.of(this.values).collect(Collectors.toList());
    }

    /**
     * Obtain mask by action string name.
     * @return Mask by string action
     */
    Function<String, Integer> maskByAction() {
        return str -> {
            for (final Action item : this.values) {
                if (item.names().contains(str)) {
                    return item.mask();
                }
            }
            throw new IllegalArgumentException(
                String.format("Unknown api repo permission action %s", str)
            );
        };
    }
}
