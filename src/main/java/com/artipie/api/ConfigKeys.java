/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.settings.ConfigFile;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Provides yaml and yml settings keys for given name.
 * @since 0.26
 */
public class ConfigKeys {
    /**
     * A pair of keys, these keys are possible settings names.
     */
    private final Pair<Key, Key> pair;

    /**
     * Ctor.
     * @param name Key name
     */
    public ConfigKeys(final String name) {
        this.pair = new ImmutablePair<>(
            new Key.From(String.format("%s.yaml", name)),
            new Key.From(String.format("%s.yml", name))
        );
    }

    /**
     * Key for setting name with '.yaml' extension.
     * @return Key
     */
    public Key yamlKey() {
        return this.pair.getLeft();
    }

    /**
     * Key for setting name with '.yml' extension.
     * @return Key
     */
    public Key ymlKey() {
        return this.pair.getRight();
    }

    /**
     * Key for setting name by YAML-extension.
     * @param ext Extension
     * @return Key
     */
    public Key key(final ConfigFile.Extension ext) {
        final Key key;
        if (ext == ConfigFile.Extension.YAML) {
            key = this.yamlKey();
        } else {
            key = this.ymlKey();
        }
        return key;
    }

    /**
     * Returns a pair of keys, these keys are possible settings names.
     * @param name Key name
     * @return Pair of keys
     */
    public Pair<Key, Key> keys(final String name) {
        return this.pair;
    }
}
