/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.test;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Empty ZIP archive for using in tests.
 * @since 0.4
 */
public final class EmptyZip {
    /**
     * Entry name. As archive should contains whatever.
     */
    private final String entry;

    /**
     * Ctor.
     */
    public EmptyZip() {
        this("whatever");
    }

    /**
     * Ctor.
     * @param entry Entry name
     */
    public EmptyZip(final String entry) {
        this.entry = entry;
    }

    /**
     * Obtains ZIP archive.
     * @return ZIP archive
     * @throws Exception In case of error during creating ZIP archive
     */
    public byte[] value() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ZipOutputStream zos = new ZipOutputStream(bos);
        zos.putNextEntry(new ZipEntry(this.entry));
        zos.close();
        return bos.toByteArray();
    }
}
