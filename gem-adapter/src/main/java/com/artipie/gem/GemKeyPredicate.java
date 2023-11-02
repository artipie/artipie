/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.gem;

import com.artipie.asto.Key;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Gem key predicate with specified names to match.
 * @since 1.3
 */
final class GemKeyPredicate implements Predicate<Key> {

    /**
     * Gem tail pattern.
     */
    private static final Pattern PTN_TAIL = Pattern.compile("^[0-9a-zA-Z\\-\\.]+\\.gem$");

    /**
     * Gem names.
     */
    private final Set<? extends String> names;

    /**
     * New predicate with one name.
     * @param name Gem name
     */
    GemKeyPredicate(final String name) {
        this(Collections.singleton(name));
    }

    /**
     * New predicate with multiple gem names.
     * @param names Gem names
     */
    GemKeyPredicate(final Set<? extends String> names) {
        this.names = names;
    }

    @Override
    public boolean test(final Key target) {
        final String gem = target.string();
        return this.names.stream().anyMatch(name -> testOne(name, gem));
    }

    /**
     * Test one item.
     * @param name Gem name
     * @param target Item target
     * @return True if target matches
     */
    private static boolean testOne(final String name, final String target) {
        final int idx = target.lastIndexOf(name);
        boolean matches = false;
        if (idx >= 0) {
            final String tail = target.substring(idx + name.length());
            if (tail.isEmpty() || PTN_TAIL.matcher(tail).matches()) {
                matches = true;
            }
        }
        return matches;
    }

}
