/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.api.artifactory;

import com.artipie.asto.Key;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class can print storage key items in different formats,
 * e.g. for Artifactory API and as list HTML page, etc.
 * @since 0.10
 */
public final class KeyList {

    /**
     * Root key.
     */
    private final Key root;

    /**
     * Key set.
     */
    private final Set<Key> keys;

    /**
     * Ctor.
     *
     * @param root Root key.
     */
    public KeyList(final Key root) {
        this.root = root;
        this.keys = new HashSet<>();
    }

    /**
     * Add key to the list.
     * @param key Key to add
     */
    public void add(final Key key) {
        this.keys.add(key);
        key.parent().ifPresent(this::add);
    }

    /**
     * Print sorted key list using specified output format.
     * @param format Output format, e.g. JSON or HTML format
     * @param <T> Format output type
     * @return Formatted result
     */
    public <T> T print(final KeysFormat<T> format) {
        final List<Key> list = new ArrayList<>(this.keys);
        list.sort(Comparator.comparing(Key::string));
        for (final Key key : list) {
            if (key.parent().map(this.root::equals).orElse(true)) {
                format.add(key, key.parent().map(this.keys::contains).orElse(false));
            }
        }
        return format.result();
    }

    /**
     * Key output format, e.g. JSON or HTML.
     * @param <T> Format output type
     * @since 0.10
     */
    public interface KeysFormat<T> {

        /**
         * Add and accumulate item.
         * @param item Key item
         * @param parent True if item is a parent of another item
         */
        void add(Key item, boolean parent);

        /**
         * Build formatted output.
         * @return Formatted output
         */
        T result();
    }
}
