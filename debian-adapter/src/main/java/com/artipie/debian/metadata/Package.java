/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.StorageValuePipeline;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Package index.
 * @since 0.1
 */
public interface Package {

    /**
     * Adds item to the packages index.
     * @param items Index items to add
     * @param index Package index key
     * @return Completion action
     */
    CompletionStage<Void> add(Iterable<String> items, Key index);

    /**
     * Simple {@link Package} implementation: it appends item to the index without any validation.
     * @since 0.1
     */
    final class Asto implements Package {

        /**
         * Package index items separator.
         */
        private static final String SEP = "\n\n";

        /**
         * Abstract storage.
         */
        private final Storage asto;

        /**
         * Ctor.
         * @param asto Storage
         */
        public Asto(final Storage asto) {
            this.asto = asto;
        }

        @Override
        public CompletionStage<Void> add(final Iterable<String> items, final Key index) {
            return CompletableFuture.supplyAsync(
                () -> String.join(Asto.SEP, items).getBytes(StandardCharsets.UTF_8)
            ).thenCompose(
                bytes -> new StorageValuePipeline<>(this.asto, index).process(
                    (opt, out) -> {
                        if (opt.isPresent()) {
                            Asto.decompressAppendCompress(opt.get(), out, bytes);
                        } else {
                            Asto.compress(bytes, out);
                        }
                    }
                )
            );
        }

        /**
         * Decompresses Packages.gz file, appends information and writes compressed result
         * into new file.
         * @param decompress File to decompress
         * @param res Where to write the result
         * @param append New bytes to append
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private static void decompressAppendCompress(
            final InputStream decompress, final OutputStream res, final byte[] append
        ) {
            try (
                OutputStream baos = new BufferedOutputStream(res);
                GzipCompressorInputStream gcis = new GzipCompressorInputStream(
                    new BufferedInputStream(decompress)
                );
                GzipCompressorOutputStream gcos =
                    new GzipCompressorOutputStream(new BufferedOutputStream(baos))
            ) {
                // @checkstyle MagicNumberCheck (1 line)
                final byte[] buf = new byte[1024];
                int cnt;
                while (-1 != (cnt = gcis.read(buf))) {
                    gcos.write(buf, 0, cnt);
                }
                gcos.write(Asto.SEP.getBytes(StandardCharsets.UTF_8));
                gcos.write(append);
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }

        /**
         * Compress text for new Package index.
         * @param bytes Bytes to compress
         * @param res Output stream to write the result
         */
        private static void compress(final byte[] bytes, final OutputStream res) {
            try (GzipCompressorOutputStream gcos =
                new GzipCompressorOutputStream(new BufferedOutputStream(res))
                ) {
                gcos.write(bytes);
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
    }

}
