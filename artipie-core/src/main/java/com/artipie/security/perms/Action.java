/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Artipie action.
 * @since 1.2
 */
public interface Action {

    /**
     * Action that implies any other action.
     */
    Action ALL = new All();

    /**
     * Action that does not imply any other action.
     */
    Action NONE = new Action() {
        @Override
        public Set<String> names() {
            return Collections.emptySet();
        }

        @Override
        public int mask() {
            return 0x0;
        }
    };

    /**
     * The list of action names.
     * @return Names list
     */
    Set<String> names();

    /**
     * Action int mask.
     * @return Mask value
     */
    int mask();

    /**
     * Standard actions.
     * @since 1.2
     * @checkstyle JavadocVariableCheck (100 lines)
     */
    enum Standard implements Action {

        READ(0x4, "read", "r", "download", "install", "pull"),

        WRITE(0x2, "write", "w", "publish", "push", "deploy", "upload"),

        DELETE(0x8, "delete", "d", "remove");

        /**
         * Action names.
         */
        private final Set<String> synonyms;

        /**
         * Action int mask.
         */
        private final int val;

        /**
         * Ctor.
         * @param value Action int mask
         * @param names Action names
         */
        Standard(final int value, final String... names) {
            this(Stream.of(names).collect(Collectors.toSet()), value);
        }

        /**
         * Ctor.
         * @param names Action names
         * @param value Action int mask
         */
        Standard(final Set<String> names, final int value) {
            this.synonyms = names;
            this.val = value;
        }

        @Override
        public Set<String> names() {
            return this.synonyms;
        }

        @Override
        public int mask() {
            return this.val;
        }

        /**
         * Get action int mask by name.
         * @param name The action name
         * @return The mask
         * @throws IllegalArgumentException is the action not valid
         */
        static int maskByAction(final String name) {
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

    /**
     * Action that implies any other action.
     * @since 1.2
     */
    final class All implements Action {

        /**
         * The action mask.
         */
        private final int mask;

        /**
         * Ctor.
         */
        private All() {
            this.mask = All.calcMask();
        }

        @Override
        public Set<String> names() {
            return Collections.singleton("*");
        }

        @Override
        public int mask() {
            return this.mask;
        }

        /**
         * Calculate action mask, the method is called only once on the first
         * call to the {@link Action#ALL} variable.
         * @return The mask
         */
        private static int calcMask() {
            final AtomicInteger res = new AtomicInteger();
            Arrays.stream(Standard.values()).map(Action::mask).forEach(
                val -> res.updateAndGet(v -> v | val)
            );
            return res.get();
        }
    }

}
