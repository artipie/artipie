/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.utils;

import com.artipie.ArtipieException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for working with gzip.
 *
 * @since 0.2
 */
public final class Gzip {

    /**
     * Data as byte array.
     */
    private final byte[] data;

    /**
     * Ctor.
      * @param data Array of bytes for gzip/unzip.
     */
    public Gzip(final byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Compresses data using gzip.
     * @return Compressed bytes in gzip format
     */
    public byte[] compress() {
        try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream(this.data.length);
            GZIPOutputStream gzipos = new GZIPOutputStream(baos, this.data.length)
        ) {
            gzipos.write(this.data);
            gzipos.finish();
            baos.flush();
            return baos.toByteArray();
        } catch (final IOException ioex) {
            throw new ArtipieException("Error when compressing gzip archive", ioex);
        }
    }

    /**
     * Decompresses data using gzip.
     * @return Decompressed bytes
     */
    public byte[] decompress() {
        try (
            GZIPInputStream gzipis = new GZIPInputStream(
                new ByteArrayInputStream(this.data),
                this.data.length
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream(this.data.length)
        ) {
            baos.writeBytes(gzipis.readAllBytes());
            return baos.toByteArray();
        } catch (final IOException ioex) {
            throw new ArtipieException("Error when decompressing gzip archive", ioex);
        }
    }
}
