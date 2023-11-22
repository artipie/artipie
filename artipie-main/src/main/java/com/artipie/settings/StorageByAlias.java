/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.settings.cache.StoragesCache;
import java.util.Optional;

/**
 * Obtain storage by alias from aliases settings yaml.
 * @since 0.4
 */
public final class StorageByAlias {

    /**
     * Aliases yaml.
     */
    private final YamlMapping yaml;

    /**
     * Aliases from yaml.
     * @param yaml Yaml
     */
    public StorageByAlias(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Get storage by alias.
     * @param cache Storage cache
     * @param alias Storage alias
     * @return Storage instance
     */
    public Storage storage(final StoragesCache cache, final String alias) {
        return Optional.ofNullable(this.yaml.yamlMapping("storages")).map(
            node -> Optional.ofNullable(node.yamlMapping(alias)).map(cache::storage)
                .orElseThrow(StorageByAlias::illegalState)
        ).orElseThrow(StorageByAlias::illegalState);
    }

    /**
     * Throws illegal state exception.
     * @return Illegal state exception.
     */
    private static RuntimeException illegalState() {
        throw new IllegalStateException(
            "yaml file with aliases is malformed or alias is absent"
        );
    }
}
