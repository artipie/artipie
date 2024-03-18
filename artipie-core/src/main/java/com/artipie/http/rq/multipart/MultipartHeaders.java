/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.misc.BufAccumulator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Multipart headers builder.
 * <p>
 * Multipart headers are created from byte-buffer chunks.
 * The chunk-receiver pushes buffers to this builder.
 * When complete, it returns this headers wrapper and
 * it lazy parses and construt headers collection.
 * After reading headers iterable, the temporary buffer
 * becomes invalid.
 */
final class MultipartHeaders {

    /**
     * Sync lock.
     */
    private final Object lock;

    /**
     * Temporary buffer accumulator.
     */
    private final BufAccumulator accumulator;

    /**
     * Headers instance cache constructed from buffer.
     */
    private volatile Headers cache;

    /**
     * New headers builder with initial capacity.
     * @param cap Initial capacity
     */
    MultipartHeaders(final int cap) {
        this.lock = new Object();
        this.accumulator = new BufAccumulator(cap);
    }

    public Headers headers() {
        if (this.cache == null) {
            synchronized (this.lock) {
                if (this.cache == null) {
                    final byte[] arr = this.accumulator.array();
                    final String hstr = new String(arr, StandardCharsets.US_ASCII);
                    this.cache = new Headers(
                        Arrays.stream(hstr.split("\r\n")).filter(str -> !str.isEmpty()).map(
                            line -> {
                                final String[] parts = line.split(":", 2);
                                return new Header(
                                    parts[0].trim().toLowerCase(Locale.US),
                                    parts[1].trim()
                                );
                            }
                        ).collect(Collectors.toList())
                    );
                }
                this.accumulator.close();
            }
        }
        return this.cache;
    }

    /**
     * Push new chunk to builder.
     * @param chunk Part of headers bytes
     */
    void push(final ByteBuffer chunk) {
        synchronized (this.lock) {
            this.accumulator.write(chunk);
        }
    }
}
