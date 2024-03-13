/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.misc.Cleanable;

/**
 * Cache for storages with similar configurations in Artipie settings.
 *
 * @since 0.23
 */
public interface StoragesCache extends Cleanable<YamlMapping> {

    StoragesLoader STORAGES = new StoragesLoader();

    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param yaml Storage settings
     * @return Storage
     */
    Storage storage(YamlMapping yaml);

    /**
     * Returns the approximate number of entries in this cache.
     *
     * @return Number of entries
     */
    long size();
}
