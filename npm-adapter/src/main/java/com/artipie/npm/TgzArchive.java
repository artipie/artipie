/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import io.reactivex.Completable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * A .tgz archive.
 *
 * @since 0.1
 */
public final class TgzArchive {

    /**
     * The archive representation in a form of a base64 string.
     */
    private final String bitstring;

    /**
     * Is Base64 encoded?
     */
    private final boolean encoded;

    /**
     * Ctor.
     * @param bitstring The archive.
     */
    public TgzArchive(final String bitstring) {
        this(bitstring, true);
    }

    /**
     * Ctor.
     * @param bitstring The archive
     * @param encoded Is Base64 encoded?
     */
    public TgzArchive(final String bitstring, final boolean encoded) {
        this.bitstring = bitstring;
        this.encoded = encoded;
    }

    /**
     * Save the archive to a file.
     *
     * @param path The path to save .tgz file at.
     * @return Completion or error signal.
     */
    public Completable saveToFile(final Path path) {
        return Completable.fromAction(
            () -> Files.write(path, this.bytes())
        );
    }

    /**
     * Obtain an archive in form of byte array.
     *
     * @return Archive bytes
     */
    public byte[] bytes() {
        final byte[] res;
        if (this.encoded) {
            res = Base64.getDecoder().decode(this.bitstring);
        } else {
            res = this.bitstring.getBytes(StandardCharsets.ISO_8859_1);
        }
        return res;
    }

    /**
     * Obtains package.json from archive.
     * @return Json object from package.json file from archive.
     */
    public JsonObject packageJson() {
        return new JsonFromStream(new ByteArrayInputStream(this.bytes())).json();
    }

    /**
     * Json input stream.
     * @since 1.5
     */
    public static class JsonFromStream {

        /**
         * Input stream to read json from.
         */
        private final InputStream input;

        /**
         * Ctor.
         * @param input Input stream to read json from
         */
        public JsonFromStream(final InputStream input) {
            this.input = input;
        }

        /**
         * Read json from tgz input stream.
         * @return Json object from stream
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        public JsonObject json() {
            try (
                GzipCompressorInputStream gzip = new GzipCompressorInputStream(this.input);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)
            ) {
                ArchiveEntry entry;
                Optional<JsonObject> json = Optional.empty();
                while ((entry = tar.getNextTarEntry()) != null) {
                    if (!tar.canReadEntryData(entry) || entry.isDirectory()) {
                        continue;
                    }
                    final String[] parts = entry.getName().split("/");
                    if ("package.json".equals(parts[parts.length - 1])) {
                        json = Optional.of(Json.createReader(tar).readObject());
                    }
                }
                return json.orElseThrow(
                    () -> new ArtipieException("'package.json' file was not found")
                );
            } catch (final IOException exc) {
                throw new ArtipieIOException(exc);
            }
        }
    }

}
