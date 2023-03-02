/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.CachedStorages;
import java.util.Optional;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class YamlSettings implements Settings {

    /**
     * YAML node name `credentials` for credentials yaml section.
     */
    public static final String NODE_CREDENTIALS = "credentials";

    /**
     * YAML node name for `ssl` yaml section.
     */
    private static final String NODE_SSL = "ssl";

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * A set of caches for artipie settings.
     */
    private final ArtipieCaches acach;

    /**
     * Metrics context.
     */
    private final MetricsContext mctx;

    /**
     * Authentication and policy.
     */
    private final ArtipieAuthorization policy;

    /**
     * Ctor.
     * @param content YAML file content.
     */
    public YamlSettings(final YamlMapping content) {
        this.content = content;
        this.policy = new ArtipieAuthorization.FromYaml(this.meta());
        this.acach = new ArtipieCaches.All(this.policy.authentication(), new CachedStorages());
        this.mctx = new MetricsContext(this.meta());
    }

    @Override
    public Storage configStorage() {
        return this.acach.storagesCache().storage(this);
    }

    @Override
    public ArtipieAuthorization authz() {
        return this.policy;
    }

    @Override
    public Layout layout() {
        return Layout.valueOf(this.meta().string("layout"));
    }

    @Override
    public YamlMapping meta() {
        return Optional.ofNullable(this.content.yamlMapping("meta"))
            .orElseThrow(
                () -> new IllegalStateException(
                    "Invalid settings: not empty `meta` section is expected"
                )
            );
    }

    @Override
    public Storage repoConfigsStorage() {
        return Optional.ofNullable(this.meta().string("repo_configs"))
            .<Storage>map(str -> new SubStorage(new Key.From(str), this.configStorage()))
            .orElse(this.configStorage());
    }

    @Override
    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(this.meta().yamlMapping(YamlSettings.NODE_SSL))
            .map(KeyStoreFactory::newInstance);
    }

    @Override
    public MetricsContext metrics() {
        return this.mctx;
    }

    @Override
    public ArtipieCaches caches() {
        return this.acach;
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

}
