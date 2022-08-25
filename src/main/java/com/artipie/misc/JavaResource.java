/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.misc;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

/**
 * Java bundled resource in {@code ./src/main/resources}.
 * @since 0.9
 */
public final class JavaResource {

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Classloader.
     */
    private final ClassLoader clo;

    /**
     * Java resource for current thread context class loader.
     * @param name Resource name
     */
    public JavaResource(final String name) {
        this(name, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Java resource.
     * @param name Resource name
     * @param clo Class loader
     */
    public JavaResource(final String name, final ClassLoader clo) {
        this.name = name;
        this.clo = clo;
    }

    /**
     * Obtain resource URI.
     * @return Resource URI
     */
    public URI uri() {
        try {
            return Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource(this.name)
            ).toURI();
        } catch (final URISyntaxException err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Copy resource data to destination.
     * @param dest Destination path
     * @throws IOException On error
     */
    public void copy(final Path dest) throws IOException {
        if (!Files.exists(dest.getParent())) {
            Files.createDirectories(dest.getParent());
        }
        try (
            InputStream src = new BufferedInputStream(
                Objects.requireNonNull(this.clo.getResourceAsStream(this.name))
            );
            OutputStream out = Files.newOutputStream(
                dest, StandardOpenOption.WRITE, StandardOpenOption.CREATE
            )
        ) {
            IOUtils.copy(src, out);
        }
        Logger.info(this, "Resource copied successfully `%s` → `%s`", this.name, dest);
    }
}
