/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import java.util.List;

/**
 * Key store.
 * @since 0.26
 */
public abstract class KeyStore {
    /**
     * YAML node name for `enabled` yaml section.
     */
    private static final String NODE_ENABLED = "enabled";

    /**
     * YAML node name for `path` yaml section.
     */
    private static final String PATH = "path";

    /**
     * YAML node name for `password` yaml section.
     */
    private static final String PASSWORD = "password";

    /**
     * YAML node name for `alias` yaml section.
     */
    private static final String ALIAS = "alias";

    /**
     * YAML node name for `aliasPassword` yaml section.
     */
    private static final String ALIAS_PASSWORD = "alias-password";

    /**
     * YAML-configuration of key store.
     */
    private final YamlMapping yml;

    /**
     * Ctor.
     * @param yaml YAML.
     */
    public KeyStore(final YamlMapping yaml) {
        this.yml = yaml;
    }

    /**
     * Checks if SSL is enabled.
     * @return True is SSL enabled.
     */
    public boolean enabled() {
        return Boolean.parseBoolean(this.yml.string(KeyStore.NODE_ENABLED));
    }

    /**
     * Create KeyStore instance.
     * @param yaml Settings of key store
     * @return KeyStore
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static KeyStore create(final YamlMapping yaml) {
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

    /**
     * Checks if configuration for this type of KeyStore is present.
     * @return True if it is configured.
     */
    public abstract boolean isConfigured();

    /**
     * Create http server.
     * @param vertx Vertx.
     * @param storage Artipie settings storage.
     * @return HttpServer
     */
    public abstract HttpServer createHttpServer(Vertx vertx, Storage storage);

    /**
     * Getter for YAML-configuration.
     * @return YAML-configuration.
     */
    YamlMapping yaml() {
        return this.yml;
    }

    /**
     * Checks if property is present in yaml.
     * @param yaml Yaml mapping.
     * @param property Property name.
     * @return True if property is present.
     */
    private static boolean hasProperty(final YamlMapping yaml, final String property) {
        return yaml.keys().stream()
            .map(yamlNode -> yamlNode.asScalar().value())
            .anyMatch(prop -> prop.equals(property));
    }

    /**
     * Gets scalar value of yaml by property name.
     * @param yaml Yaml mapping.
     * @param property Property name.
     * @return Scalar value.
     */
    private static String property(final YamlMapping yaml, final String property) {
        return yaml.string(property);
    }

    /**
     * Gets node of yaml by property name.
     * @param yaml Yaml mapping.
     * @param property Property name.
     * @return Node.
     */
    private static YamlMapping node(final YamlMapping yaml, final String property) {
        return yaml.yamlMapping(property);
    }

    /**
     * Reads key value from storage by path.
     * @param storage Storage.
     * @param path Path for storage key.
     * @return Kye value by specified path.
     */
    private static Buffer read(final Storage storage, final String path) {
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
     * JKS key store.
     * @since 0.26
     */
    private static class JksKeyStore extends KeyStore {
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
            if (hasProperty(jks, KeyStore.PASSWORD)) {
                options.setPassword(property(jks, KeyStore.PASSWORD));
            }
            if (hasProperty(jks, KeyStore.ALIAS)) {
                options.setAlias(property(jks, KeyStore.ALIAS));
            }
            if (hasProperty(jks, KeyStore.ALIAS_PASSWORD)) {
                options.setAliasPassword(property(jks, KeyStore.ALIAS_PASSWORD));
            }
            if (hasProperty(jks, KeyStore.PATH)) {
                options.setValue(read(storage, property(jks, KeyStore.PATH)));
            }
            return options;
        }
    }

    /**
     * PEM key store.
     * @since 0.26
     */
    private static class PemKeyStore extends KeyStore {
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
            if (hasProperty(pem, PemKeyStore.KEY_PATH)) {
                options.setKeyValue(read(storage, property(pem, PemKeyStore.KEY_PATH)));
            }
            if (hasProperty(pem, PemKeyStore.CERT_PATH)) {
                options.setCertValue(read(storage, property(pem, PemKeyStore.CERT_PATH)));
            }
            return options;
        }
    }

    /**
     * PEM key store.
     * @since 0.26
     */
    private static class PfxKeyStore extends KeyStore {
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
        public HttpServer createHttpServer(final Vertx vertx, final Storage storage) {
            final HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setPfxKeyCertOptions(this.pfxOptions(storage));
            return vertx.createHttpServer(options);
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
            if (hasProperty(pfx, KeyStore.PASSWORD)) {
                options.setPassword(property(pfx, KeyStore.PASSWORD));
            }
            if (hasProperty(pfx, KeyStore.ALIAS)) {
                options.setAlias(property(pfx, KeyStore.ALIAS));
            }
            if (hasProperty(pfx, KeyStore.ALIAS_PASSWORD)) {
                options.setAliasPassword(property(pfx, KeyStore.ALIAS_PASSWORD));
            }
            if (hasProperty(pfx, KeyStore.PATH)) {
                options.setValue(read(storage, property(pfx, KeyStore.PATH)));
            }
            return options;
        }
    }
}
