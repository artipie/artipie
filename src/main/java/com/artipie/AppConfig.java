/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.settings.Settings;
import com.artipie.settings.SettingsFromPath;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Application configuration.
 * @since 0.26
 */
public interface AppConfig {

    /**
     * Obtain Artipie settings.
     * @return Instance of {@link Settings}
     */
    Settings setting();

    /**
     * Client slices for proxy adapters.
     * @return Instance of {@link ClientSlices}
     */
    ClientSlices httpClient();

    /**
     * Port to start Vert.x server on. Default value is 80.
     * @return Vertx port
     */
    int vertxPort();

    /**
     * Configuration from environment.
     * @since 0.26
     */
    @Configuration
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    class FromEnv implements AppConfig {

        /**
         * Environment.
         */
        private final Environment env;

        /**
         * Ctor.
         * @param env Application environment
         */
        public FromEnv(final Environment env) {
            this.env = env;
        }

        @Override
        @SuppressWarnings("PMD.AvoidCatchingNPE")
        public Settings setting() {
            try {
                final String fopt = this.env.getProperty("f");
                final String longopt = this.env.getProperty("config-file");
                return new SettingsFromPath(
                    Path.of(
                        Objects.requireNonNull(Optional.ofNullable(fopt).orElse(longopt))
                    )
                ).find(this.vertxPort());
            } catch (final IOException err) {
                throw new UncheckedIOException(
                    "Failed to parse main Artipie configuration file", err
                );
            } catch (final NullPointerException err) {
                // @checkstyle LineLengthCheck (1 line)
                Logger.error(this, "Main Artipie configuration file path is not configured, specify path with --config-file or --f program arguments");
                throw err;
            }
        }

        @Override
        public ClientSlices httpClient() {
            try {
                final JettyClientSlices http = new JettyClientSlices(new HttpClientSettings());
                http.start();
                return http;
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final Exception err) {
                throw new IllegalStateException("Failed to start HTTP client", err);
            }
        }

        @Override
        public int vertxPort() {
            return this.env.getProperty(
                "p", Integer.class, this.env.getProperty("port", Integer.class, 80)
            );
        }
    }
}
