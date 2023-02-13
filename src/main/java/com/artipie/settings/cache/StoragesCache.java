/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.Storage;
import com.artipie.settings.Settings;
import com.artipie.settings.StorageByAlias;

/**
 * Cache for storages with similar configurations in Artipie settings.
 * @since 0.23
 */
public interface StoragesCache extends Cleanable {
    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param settings Artipie settings
     * @return Storage
     */
    Storage storage(Settings settings);

    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param yaml Storage settings
     * @return Storage
     */
    Storage storage(YamlMapping yaml);

    /**
     * Get storage by yaml node taking aliases into account.
     *
     * @param aliases Storage by alias
     * @param node Storage config yaml node
     * @return Storage instance
     */
    default Storage storage(StorageByAlias aliases, YamlNode node) {
        final Storage res;
        if (node instanceof Scalar) {
            res = aliases.storage(this, ((Scalar) node).value());
        } else if (node instanceof YamlMapping) {
            res = this.storage((YamlMapping) node);
        } else {
            throw new IllegalStateException(
                String.format("Invalid storage config: %s", node)
            );
        }
        return res;
    }

    /**
     * Returns the approximate number of entries in this cache.
     *
     * @return Number of entries
     */
    long size();

}
