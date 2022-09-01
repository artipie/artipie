/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.Json2Yaml;
import com.artipie.misc.Yaml2Json;
import com.artipie.settings.CrudStorageAliases;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.JsonObject;

/**
 * Manage storage aliases settings.
 * @since 0.27
 */
public final class ManageStorageAliases implements CrudStorageAliases {

    /**
     * Settings file name.
     */
    public static final String FILE_NAME = "_storages";

    /**
     * Key for the settings file with .yaml extension.
     */
    private static final Key YAML = new Key.From(
        String.format("%s.yaml", ManageStorageAliases.FILE_NAME)
    );

    /**
     * Yaml storages section name.
     */
    private static final String STORAGES_NODE = "storages";

    /**
     * Repository or user key.
     */
    private final Optional<Key> key;

    /**
     * Storage.
     */
    private final BlockingStorage blsto;

    /**
     * Ctor.
     * @param key Repository or user key
     * @param blsto Storage
     */
    public ManageStorageAliases(final Optional<Key> key, final BlockingStorage blsto) {
        this.key = key;
        this.blsto = blsto;
    }

    /**
     * Ctor.
     * @param key Repository or user key
     * @param blsto Storage
     */
    public ManageStorageAliases(final Key key, final BlockingStorage blsto) {
        this(Optional.of(key), blsto);
    }

    /**
     * Ctor.
     * @param blsto Storage
     */
    public ManageStorageAliases(final BlockingStorage blsto) {
        this(Optional.empty(), blsto);
    }

    @Override
    public Collection<? extends StorageAlias> list() {
        final Optional<YamlMapping> storages = this.storages();
        return storages.map(
            nodes -> nodes.keys().stream().map(node -> node.asScalar().value()).map(
                alias -> new YamlStorage(alias, storages.get().yamlMapping(alias))
            ).collect(Collectors.toList())
        ).orElse(Collections.emptyList());
    }

    @Override
    public void add(final String alias, final JsonObject info) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        final Optional<YamlMapping> storages = this.storages();
        if (storages.isPresent()) {
            for (final YamlNode node : storages.get().keys()) {
                final String name = node.asScalar().value();
                builder = builder.add(name, storages.get().yamlMapping(name));
            }
        }
        builder = builder.add(alias, new Json2Yaml().apply(info.toString()));
        this.blsto.save(
            this.settingKey().orElse(
                this.key.<Key>map(val -> new Key.From(val, ManageStorageAliases.YAML))
                    .orElse(ManageStorageAliases.YAML)
            ),
            Yaml.createYamlMappingBuilder().add(ManageStorageAliases.STORAGES_NODE, builder.build())
                .build().toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void remove(final String alias) {
        final Optional<YamlMapping> storages = this.storages();
        if (storages.isPresent() && storages.get().value(alias) != null) {
            YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
            for (final YamlNode node : storages.get().keys()) {
                final String name = node.asScalar().value();
                if (!alias.equals(name)) {
                    builder = builder.add(name, storages.get().yamlMapping(name));
                }
            }
            this.blsto.save(
                this.settingKey().get(),
                Yaml.createYamlMappingBuilder()
                    .add(ManageStorageAliases.STORAGES_NODE, builder.build())
                    .build().toString().getBytes(StandardCharsets.UTF_8)
            );
            return;
        }
        throw new IllegalStateException(String.format("Storage alias %s does not exist", alias));
    }

    /**
     * Returns storages yaml mapping if found.
     * @return Settings storages yaml
     */
    private Optional<YamlMapping> storages() {
        final Optional<Key> stng = this.settingKey();
        Optional<YamlMapping> res = Optional.empty();
        if (stng.isPresent()) {
            try {
                res = Optional.ofNullable(
                    Yaml.createYamlInput(
                        new String(this.blsto.value(stng.get()), StandardCharsets.UTF_8)
                    ).readYamlMapping().yamlMapping(ManageStorageAliases.STORAGES_NODE)
                );
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
        return res;
    }

    /**
     * Finds storages settings key.
     * @return The key if found
     */
    private Optional<Key> settingKey() {
        Optional<Key> res = Optional.of(
            this.key.<Key>map(val -> new Key.From(val, ManageStorageAliases.YAML))
                .orElse(ManageStorageAliases.YAML)
        );
        if (!this.blsto.exists(res.get())) {
            final String yml = String.format("%s.yml", ManageStorageAliases.FILE_NAME);
            res = Optional.of(
                this.key.map(val -> new Key.From(val, yml)).orElse(new Key.From(yml))
            );
            if (!this.blsto.exists(res.get())) {
                res = Optional.empty();
            }
        }
        return res;
    }

    /**
     * Implementation of {@link StorageAlias} from Yaml.
     * @since 0.1
     */
    static final class YamlStorage implements StorageAlias {

        /**
         * Storage alias name.
         */
        private final String name;

        /**
         * Storage yaml mapping.
         */
        private final YamlMapping yaml;

        /**
         * Ctor.
         * @param name Storage alias name
         * @param yaml Storage yaml mapping
         */
        YamlStorage(final String name, final YamlMapping yaml) {
            this.name = name;
            this.yaml = yaml;
        }

        @Override
        public String alias() {
            return this.name;
        }

        @Override
        public JsonObject info() {
            return new Yaml2Json().apply(this.yaml.toString()).asJsonObject();
        }
    }
}
