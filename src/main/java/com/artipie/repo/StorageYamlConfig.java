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
package com.artipie.repo;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.StorageAliases;
import com.artipie.YamlStorage;
import com.artipie.asto.Key;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import java.util.logging.Level;

/**
 * Processes yaml for creating storage instance.
 * @since 0.14
 */
public final class StorageYamlConfig {

    /**
     * Yaml node for storage section.
     */
    private final YamlNode node;

    /**
     * Storage aliases.
     */
    private final StorageAliases aliases;

    /**
     * Ctor.
     * @param node Yaml node for storage section
     * @param aliases Storage aliases
     */
    public StorageYamlConfig(final YamlNode node, final StorageAliases aliases) {
        this.node = node;
        this.aliases = aliases;
    }

    /**
     * SubStorage with specified prefix.
     * @param prefix Storage prefix
     * @return SubStorage with specified prefix.
     */
    public Storage subStorage(final Key prefix) {
        return new SubStorage(prefix, new LoggingStorage(Level.INFO, this.storage()));
    }

    /**
     * Storage instance from yaml.
     * @return Storage.
     */
    public Storage storage() {
        final Storage storage;
        if (this.node instanceof Scalar) {
            storage = this.aliases.storage(((Scalar) this.node).value());
        } else if (this.node instanceof YamlMapping) {
            storage = new YamlStorage((YamlMapping) this.node).storage();
        } else {
            throw new IllegalStateException(
                String.format("Invalid storage config: %s", this.node)
            );
        }
        return storage;
    }
}
