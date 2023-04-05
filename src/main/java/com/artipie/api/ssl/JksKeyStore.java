/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.api.ssl;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

/**
 * JKS key store.
 * @since 0.26
 */
class JksKeyStore extends YamlBasedKeyStore {
    /**
     * YAML node name for `jks` yaml section.
     */
    private static final String JKS = "jks";

    /**
     * Ctor.
     * @param yamlConfig YAML.
     */
    JksKeyStore(final YamlMapping yamlConfig) {
        super(yamlConfig);
    }

    @Override
    public boolean isConfigured() {
        return hasProperty(this.yaml(), JksKeyStore.JKS);
    }

    @Override
    public HttpServerOptions secureOptions(final Vertx vertx, final Storage storage) {
        return new HttpServerOptions()
                .setSsl(true)
                .setKeyStoreOptions(this.jksOptions(storage));
    }

    /**
     * Initialize JKS-options based on yaml-configuration.
     * @param storage Storage.
     * @return JKS-options for http server.
     */
    private JksOptions jksOptions(final Storage storage) {
        if (!hasProperty(this.yaml(), JksKeyStore.JKS)) {
            throw new IllegalStateException("'jks'-section is expected in yaml-configuration");
        }
        final YamlMapping jks = node(this.yaml(), JksKeyStore.JKS);
        final JksOptions options = new JksOptions();

        final String password = YamlBasedKeyStore.PASSWORD;
        final String alias = YamlBasedKeyStore.ALIAS;
        final String aliasPassword = YamlBasedKeyStore.ALIAS_PASSWORD;
        final String path = YamlBasedKeyStore.PATH;

        YamlBasedKeyStore.setIfExists(jks, password, options::setPassword);
        YamlBasedKeyStore.setIfExists(jks, alias, options::setAlias);
        YamlBasedKeyStore.setIfExists(jks, aliasPassword, options::setAliasPassword);
        YamlBasedKeyStore.setIfExists(jks, path, filePath -> options.setValue(read(storage, filePath)));

        return options;
    }
}

