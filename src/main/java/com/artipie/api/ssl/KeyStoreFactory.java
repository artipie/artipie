/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.ssl;

import com.amihaiemil.eoyaml.YamlMapping;
import java.util.List;

/**
 * KeyStore factory.
 * @since 0.26
 */
public final class KeyStoreFactory {
    /**
     * Ctor.
     */
    private KeyStoreFactory() {
    }

    /**
     * Create KeyStore instance.
     * @param yaml Settings of key store
     * @return KeyStore
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static KeyStore newInstance(final YamlMapping yaml) {
        final List<KeyStore> keystores = List.of(
            new JksKeyStore(yaml), new PemKeyStore(yaml), new PfxKeyStore(yaml)
        );
        for (final KeyStore keystore : keystores) {
            if (keystore.isConfigured()) {
                return keystore;
            }
        }
        throw new IllegalStateException("Not found configuration in 'ssl'-section of yaml");
    }
}
