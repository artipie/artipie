/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.publish;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.ParameterParser;
import org.reactivestreams.Publisher;

/**
 * HTTP 'multipart/form-data' request.
 *
 * @since 0.1
 */
final class Multipart {

    /**
     * Size of multipart stream buffer.
     */
    private static final int BUFFER = 4096;

    /**
     * Request headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Request body.
     */
    private final Publisher<ByteBuffer> body;

    /**
     * Ctor.
     *
     * @param headers Request headers.
     * @param body Request body.
     */
    Multipart(
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        this.headers = headers;
        this.body = body;
    }

    /**
     * Read first part.
     *
     * @return First part content.
     */
    public Content first() {
        return new Content.From(
            new Concatenation(this.body)
                .single()
                .map(Remaining::new)
                .map(Remaining::bytes)
                .map(ByteArrayInputStream::new)
                .map(input -> new MultipartStream(input, this.boundary(), Multipart.BUFFER, null))
                .map(Multipart::first)
                .toFlowable()
        );
    }

    /**
     * Reads boundary from headers.
     *
     * @return Boundary bytes.
     */
    private byte[] boundary() {
        final String header = StreamSupport.stream(this.headers.spliterator(), false)
            .filter(entry -> "Content-Type".equalsIgnoreCase(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Cannot find header \"Content-Type\"")
            );
        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        final String boundary = Objects.requireNonNull(
            parser.parse(header, ';').get("boundary"),
            String.format("Boundary not specified: '%s'", header)
        );
        return boundary.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Read first part from stream.
     *
     * @param stream Multipart stream.
     * @return Binary content of first part.
     */
    private static ByteBuffer first(final MultipartStream stream) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            if (!stream.skipPreamble()) {
                throw new IllegalStateException("Body has no parts");
            }
            stream.readHeaders();
            stream.readBodyData(bos);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read body as multipart", ex);
        }
        return ByteBuffer.wrap(bos.toByteArray());
    }
}
