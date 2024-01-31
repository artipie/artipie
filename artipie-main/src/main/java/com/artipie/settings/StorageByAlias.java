/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.settings.cache.StoragesCache;

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
        final YamlMapping mapping = this.yaml.yamlMapping("storages");
        if (mapping != null) {
            final YamlMapping aliasMapping = mapping.yamlMapping(alias);
            if (aliasMapping != null) {
                return cache.storage(aliasMapping);
            }
        }
        throw new IllegalStateException(
            String.format(
                "yaml file with aliases is malformed or alias `%s` is absent",
                alias
            )
        );
    }
}
