/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.ssl;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import io.vertx.core.buffer.Buffer;
import java.util.function.Consumer;

/**
 * Yaml based KeyStore.
 * @since 0.26
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
public abstract class YamlBasedKeyStore implements KeyStore {
    /**
     * YAML node name for `path` yaml section.
     */
    protected static final String PATH = "path";

    /**
     * YAML node name for `password` yaml section.
     */
    protected static final String PASSWORD = "password";

    /**
     * YAML node name for `alias` yaml section.
     */
    protected static final String ALIAS = "alias";

    /**
     * YAML node name for `aliasPassword` yaml section.
     */
    protected static final String ALIAS_PASSWORD = "alias-password";

    /**
     * YAML-configuration of key store.
     */
    private final YamlMapping yml;

    /**
     * Ctor.
     * @param yaml YAML.
     */
    public YamlBasedKeyStore(final YamlMapping yaml) {
        this.yml = yaml;
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(this.yml.string("enabled"));
    }

    /**
     * Getter for YAML-configuration.
     * @return YAML-configuration.
     */
    protected YamlMapping yaml() {
        return this.yml;
    }

    /**
     * Checks if property is present in yaml.
     * @param yaml Yaml mapping.
     * @param property Property name.
     * @return True if property is present.
     */
    protected static boolean hasProperty(final YamlMapping yaml, final String property) {
        return yaml.keys().stream()
            .map(yamlNode -> yamlNode.asScalar().value())
            .anyMatch(prop -> prop.equals(property));
    }

    /**
     * Gets node of yaml by property name.
     * @param yaml Yaml mapping.
     * @param property Property name.
     * @return Node.
     */
    protected static YamlMapping node(final YamlMapping yaml, final String property) {
        return yaml.yamlMapping(property);
    }

    /**
     * Reads key value from storage by path.
     * @param storage Storage.
     * @param path Path for storage key.
     * @return Kye value by specified path.
     */
    protected static Buffer read(final Storage storage, final String path) {
        final Key key = new Key.From(path);
        final BlockingStorage bstg = new BlockingStorage(storage);
        if (bstg.exists(key)) {
            return Buffer.buffer(bstg.value(key));
        } else {
            throw new IllegalArgumentException(
                String.format("Path %s does not exists in storage", path)
            );
        }
    }

    /**
     * Invokes setter if property is present in yaml.
     * @param yaml Yaml mapping.
     * @param prop Property name.
     * @param setter Setter-code of yaml-property.
     */
    protected static void setIfExists(final YamlMapping yaml, final String prop,
        final Consumer<String> setter) {
        if (hasProperty(yaml, prop)) {
            setter.accept(yaml.string(prop));
        }
    }
}
