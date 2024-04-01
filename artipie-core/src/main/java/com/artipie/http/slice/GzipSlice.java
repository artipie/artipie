/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveInputStream;
import org.cqfn.rio.stream.ReactiveOutputStream;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPOutputStream;

/**
 * Slice that gzips requested content.
 */
final class GzipSlice implements Slice {

    private final Slice origin;

    /**
     * @param origin Origin slice
     */
    GzipSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        return this.origin.response(line, headers, body)
            .thenCompose(
                r -> gzip(r.body()).thenApply(
                    content -> ResponseBuilder.from(r.status())
                        .headers(r.headers())
                        .header("Content-encoding", "gzip")
                        .body(content)
                        .build()
                )
            );
    }

    @SuppressWarnings("PMD.CloseResource")
    private static CompletionStage<Content> gzip(Publisher<ByteBuffer> body) {
        CompletionStage<Content> res;
        try (PipedOutputStream resout = new PipedOutputStream();
            PipedInputStream oinput = new PipedInputStream();
             PipedOutputStream tmpout = new PipedOutputStream(oinput)) {
            final PipedInputStream src = new PipedInputStream(resout);
            CompletableFuture.allOf(
                new ReactiveOutputStream(tmpout)
                    .write(body, WriteGreed.SYSTEM)
                    .toCompletableFuture()
            );
            res = CompletableFuture.supplyAsync(
                () -> new Content.From(new ReactiveInputStream(src).read(Buffers.Standard.K8))
            );
            try (GZIPOutputStream gzos = new GZIPOutputStream(resout)) {
                final byte[] buffer = new byte[1024 * 8];
                while (true) {
                    final int length = oinput.read(buffer);
                    if (length < 0) {
                        break;
                    }
                    gzos.write(buffer, 0, length);
                }
                gzos.finish();
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
        return res;
    }
}
