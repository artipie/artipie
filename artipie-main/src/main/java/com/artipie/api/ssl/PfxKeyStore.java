/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.ssl;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;

/**
 * PEM key store.
 * @since 0.26
 */
class PfxKeyStore extends YamlBasedKeyStore {
    /**
     * YAML node name for `pem` yaml section.
     */
    private static final String PFX = "pfx";

    /**
     * Ctor.
     * @param yaml YAML.
     */
    PfxKeyStore(final YamlMapping yaml) {
        super(yaml);
    }

    @Override
    public boolean isConfigured() {
        return hasProperty(this.yaml(), PfxKeyStore.PFX);
    }

    @Override
    public HttpServerOptions secureOptions(final Vertx vertx, final Storage storage) {
        return new HttpServerOptions()
            .setSsl(true)
            .setPfxKeyCertOptions(this.pfxOptions(storage));
    }

    /**
     * Initialize PFX-options based on yaml-configuration.
     * @param storage Storage.
     * @return PFX-options for http server.
     */
    private PfxOptions pfxOptions(final Storage storage) {
        if (!hasProperty(this.yaml(), PfxKeyStore.PFX)) {
            throw new IllegalStateException("'pfx'-section is expected in yaml-configuration");
        }
        final YamlMapping pfx = node(this.yaml(), PfxKeyStore.PFX);
        final PfxOptions options = new PfxOptions();
        YamlBasedKeyStore.setIfExists(pfx, YamlBasedKeyStore.PASSWORD, options::setPassword);
        YamlBasedKeyStore.setIfExists(pfx, YamlBasedKeyStore.ALIAS, options::setAlias);
        YamlBasedKeyStore.setIfExists(
            pfx,
            YamlBasedKeyStore.ALIAS_PASSWORD,
            options::setAliasPassword
        );
        YamlBasedKeyStore.setIfExists(
            pfx,
            YamlBasedKeyStore.PATH,
            path -> options.setValue(read(storage, path))
        );
        return options;
    }
}
