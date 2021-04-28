/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.jcabi.log.Logger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
