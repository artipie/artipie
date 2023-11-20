/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import java.nio.charset.StandardCharsets;

/**
 * GzArchive: packs or unpacks.
 * @since 0.6
 */
public final class AstoGzArchive {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public AstoGzArchive(final Storage asto) {
        this.asto = asto;
    }

    /**
     * Compress provided bytes in gz format and adds item to storage by provided key.
     * @param bytes Bytes to pack
     * @param key Storage key
     */
    public void packAndSave(final byte[] bytes, final Key key) {
        this.asto.save(key, new Content.From(new GzArchive().compress(bytes))).join();
    }

    /**
     * Compress provided string in gz format and adds item to storage by provided key.
     * @param content String to pack
     * @param key Storage key
     */
    public void packAndSave(final String content, final Key key) {
        this.packAndSave(content.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * Unpacks storage item and returns unpacked content as string.
     * @param key Storage item
     * @return Unpacked string
     */
    public String unpack(final Key key) {
        return new GzArchive().decompress(new BlockingStorage(this.asto).value(key));
    }
}
