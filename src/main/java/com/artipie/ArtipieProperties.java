/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.util.Properties;

/**
 * Artipie properties.
 * @since 0.21
 */
@SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
public final class ArtipieProperties {
    /**
     * Key of field which contains Artipie version.
     */
    public static final String VERSION_KEY = "artipie.version";

    /**
     * Name of file with properties.
     */
    private final String filename;

    /**
     * Properties.
     */
    private final Properties properties;

    /**
     * Ctor with default name of file with properties.
     */
    public ArtipieProperties() {
        this("artipie.properties");
    }

    /**
     * Ctor.
     * @param filename Filename with properties
     */
    public ArtipieProperties(final String filename) {
        this.filename = filename;
        this.properties = new Properties();
        this.loadProperties();
    }

    /**
     * Obtains version of Artipie.
     * @return Version
     */
    public String version() {
        return this.properties.getProperty(ArtipieProperties.VERSION_KEY);
    }

    /**
     * Load content of file.
     */
    private void loadProperties() {
        try {
            this.properties.load(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(this.filename)
            );
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
    }
}
