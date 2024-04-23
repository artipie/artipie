/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Direct children keys for root from collection of keys.
 */
public class Children {

    /**
     * Root key.
     */
    private final Key root;

    /**
     * List of keys inside root.
     */
    private final Collection<Key> keys;

    /**
     * @param root Root key.
     * @param keys List of keys inside root.
     */
    public Children(final Key root, final Collection<Key> keys) {
        this.root = root;
        this.keys = keys;
    }

    /**
     * Extract unique child names in lexicographical order.
     *
     * @return Ordered child names.
     */
    public Set<String> names() {
        final Set<String> set = new TreeSet<>();
        for (final Key key : this.keys) {
            set.add(this.child(key));
        }
        return set;
    }

    /**
     * Extract direct root child node from key.
     *
     * @param key Key.
     * @return Direct child name.
     */
    private String child(final Key key) {
        Key child = key;
        while (true) {
            final Optional<Key> parent = child.parent();
            if (parent.isEmpty()) {
                throw new IllegalStateException(
                    String.format("Key %s does not belong to root %s", key, this.root)
                );
            }
            if (parent.get().string().equals(this.root.string())) {
                break;
            }
            child = parent.get();
        }
        return child.string().substring(this.root.string().length() + 1);
    }
}
