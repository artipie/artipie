/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
