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
import io.vertx.core.net.PemKeyCertOptions;

/**
 * PEM key store.
 * @since 0.26
 */
class PemKeyStore extends YamlBasedKeyStore {
    /**
     * YAML node name for `pem` yaml section.
     */
    private static final String PEM = "pem";

    /**
     * YAML node name for `key-path` yaml section.
     */
    private static final String KEY_PATH = "key-path";

    /**
     * YAML node name for `cert-path` yaml section.
     */
    private static final String CERT_PATH = "cert-path";

    /**
     * Ctor.
     * @param yaml YAML.
     */
    PemKeyStore(final YamlMapping yaml) {
        super(yaml);
    }

    @Override
    public boolean isConfigured() {
        return hasProperty(this.yaml(), PemKeyStore.PEM);
    }

    @Override
    public HttpServer createHttpServer(final Vertx vertx, final Storage storage) {
        final HttpServerOptions options = new HttpServerOptions()
            .setSsl(true)
            .setPemKeyCertOptions(this.pemOptions(storage));
        return vertx.createHttpServer(options);
    }

    /**
     * Initialize PEM-options based on yaml-configuration.
     * @param storage Storage.
     * @return PEM-options for http server.
     */
    private PemKeyCertOptions pemOptions(final Storage storage) {
        if (!hasProperty(this.yaml(), PemKeyStore.PEM)) {
            throw new IllegalStateException("'pem'-section is expected in yaml-configuration");
        }
        final YamlMapping pem = node(this.yaml(), PemKeyStore.PEM);
        final PemKeyCertOptions options = new PemKeyCertOptions();
        YamlBasedKeyStore.setIfExists(
            pem,
            PemKeyStore.KEY_PATH,
            keypath -> options.setKeyValue(read(storage, keypath))
        );
        YamlBasedKeyStore.setIfExists(
            pem,
            PemKeyStore.CERT_PATH,
            certpath -> options.setCertValue(read(storage, certpath))
        );
        return options;
    }
}
