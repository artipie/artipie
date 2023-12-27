/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Read bytes from content to memory.
 * Using this class keep in mind that it reads ByteBuffer from publisher into memory and is not
 * suitable for large content.
 * @since 0.24
 */
public final class PublisherAs {

    /**
     * Content to read bytes from.
     */
    private final Content content;

    /**
     * Ctor.
     * @param content Content
     */
    public PublisherAs(final Content content) {
        this.content = content;
    }

    /**
     * Ctor.
     * @param content Content
     */
    public PublisherAs(final Publisher<ByteBuffer> content) {
        this(new Content.From(content));
    }

    /**
     * Reads bytes from content into memory.
     * @return Byte array as CompletionStage
     */
    public CompletionStage<byte[]> bytes() {
        return new Concatenation(this.content)
            .single()
            .map(buf -> new Remaining(buf, true))
            .map(Remaining::bytes)
            .to(SingleInterop.get());
    }

    /**
     * Reads bytes from content as string.
     * @param charset Charset to read string
     * @return String as CompletionStage
     */
    public CompletionStage<String> string(final Charset charset) {
        return this.bytes().thenApply(bytes -> new String(bytes, charset));
    }

    /**
     * Reads bytes from content as {@link StandardCharsets#US_ASCII} string.
     * @return String as CompletionStage
     */
    public CompletionStage<String> asciiString() {
        return this.string(StandardCharsets.US_ASCII);
    }

}
