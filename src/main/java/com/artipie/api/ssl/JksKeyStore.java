/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.ssl;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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
     * @param yaml YAML.
     */
    JksKeyStore(final YamlMapping yaml) {
        super(yaml);
    }

    @Override
    public boolean isConfigured() {
        return hasProperty(this.yaml(), JksKeyStore.JKS);
    }

    @Override
    public HttpServer createHttpServer(final Vertx vertx, final Storage storage) {
        final HttpServerOptions options = new HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(this.jksOptions(storage));
        return vertx.createHttpServer(options);
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
        YamlBasedKeyStore.setIfExists(jks, YamlBasedKeyStore.PASSWORD, options::setPassword);
        YamlBasedKeyStore.setIfExists(jks, YamlBasedKeyStore.ALIAS, options::setAlias);
        YamlBasedKeyStore.setIfExists(
            jks,
            YamlBasedKeyStore.ALIAS_PASSWORD,
            options::setAliasPassword
        );
        YamlBasedKeyStore.setIfExists(
            jks,
            YamlBasedKeyStore.PATH,
            path -> options.setValue(read(storage, path))
        );
        return options;
    }
}

