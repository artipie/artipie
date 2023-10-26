/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Class to work with gz: pack and unpack bytes.
 * @since 0.4
 * @checkstyle NonStaticMethodCheck (500 lines)
 */
public final class GzArchive {

    /**
     * Compresses provided bytes in gz format.
     * @param data Bytes to pack
     * @return Packed bytes
     */
    public byte[] compress(final byte[] data) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream gcos =
            new GzipCompressorOutputStream(new BufferedOutputStream(baos))) {
            gcos.write(data);
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
        return baos.toByteArray();
    }

    /**
     * Decompresses provided gz packed data.
     * @param data Bytes to unpack
     * @return Unpacked data in string format
     * @checkstyle MagicNumberCheck (15 lines)
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public String decompress(final byte[] data) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (
            GzipCompressorInputStream gcis = new GzipCompressorInputStream(
                new BufferedInputStream(new ByteArrayInputStream(data))
            )
        ) {
            final byte[] buf = new byte[1024];
            int cnt;
            while (-1 != (cnt = gcis.read(buf))) {
                out.write(buf, 0, cnt);
            }
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
        return out.toString();
    }
}
