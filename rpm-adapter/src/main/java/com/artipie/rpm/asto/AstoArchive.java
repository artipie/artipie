/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.StorageValuePipeline;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPOutputStream;

/**
 * Archive storage item.
 * @since 1.9
 */
final class AstoArchive {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Asto storage
     */
    AstoArchive(final Storage asto) {
        this.asto = asto;
    }

    /**
     * Compress storage item with gzip compression.
     * @param key Item to gzip
     * @return Completable action
     */
    public CompletionStage<Void> gzip(final Key key) {
        return new StorageValuePipeline<>(this.asto, key).process(
            (inpt, out) -> {
                try (GZIPOutputStream gzos = new GZIPOutputStream(out)) {
                    // @checkstyle MagicNumberCheck (1 line)
                    final byte[] buffer = new byte[1024 * 8];
                    while (true) {
                        final int length = inpt.get().read(buffer);
                        if (length < 0) {
                            break;
                        }
                        gzos.write(buffer, 0, length);
                    }
                    gzos.finish();
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        );
    }
}
