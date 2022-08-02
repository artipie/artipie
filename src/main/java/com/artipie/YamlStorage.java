/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Storages;
import com.google.common.base.Strings;

/**
 * Storage settings built from YAML.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class YamlStorage {

    /**
     * YAML storage settings.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml YAML storage settings.
     */
    public YamlStorage(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Provides a storage.
     *
     * @return Storage instance.
     */
    public Storage storage() {
        final String type = this.yaml.string("type");
        if (Strings.isNullOrEmpty(type)) {
            throw new IllegalArgumentException("Storage type cannot be null or empty.");
        }
        return new Storages().newStorage(type, this.yaml);
    }
}
