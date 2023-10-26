/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.slice.KeyFromPath;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Gpg configuration.
 * @since 0.4
 */
public interface GpgConfig {

    /**
     * Password to unlock gpg-private key.
     * @return String password
     */
    String password();

    /**
     * Gpg-private key.
     * @return Completion action with key bytes
     */
    CompletionStage<byte[]> key();

    /**
     * Gpg-configuration from yaml settings.
     * @since 0.4
     */
    final class FromYaml implements GpgConfig {

        /**
         * Gpg password field name.
         */
        static final String GPG_PASSWORD = "gpg_password";

        /**
         * Gpg secret key path field name.
         */
        static final String GPG_SECRET_KEY = "gpg_secret_key";

        /**
         * Setting in yaml format.
         */
        private final YamlMapping yaml;

        /**
         * Artipie configuration storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param yaml Yaml `settings` section
         * @param storage Artipie configuration storage
         */
        public FromYaml(final Optional<YamlMapping> yaml, final Storage storage) {
            this(
                yaml.orElseThrow(
                    () -> new IllegalArgumentException(
                        "Illegal config: `setting` section is required for debian repos"
                    )
                ),
                storage
            );
        }

        /**
         * Ctor.
         * @param yaml Yaml `settings` section
         * @param storage Artipie configuration storage
         */
        public FromYaml(final YamlMapping yaml, final Storage storage) {
            this.yaml = yaml;
            this.storage = storage;
        }

        @Override
        public String password() {
            return this.yaml.string(FromYaml.GPG_PASSWORD);
        }

        @Override
        public CompletionStage<byte[]> key() {
            return this.storage.value(new KeyFromPath(this.yaml.string(FromYaml.GPG_SECRET_KEY)))
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::bytes);
        }
    }
}
