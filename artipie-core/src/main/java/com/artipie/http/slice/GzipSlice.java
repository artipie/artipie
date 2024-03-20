/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
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
 * @since 1.1
 */
final class GzipSlice implements Slice {

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
     * @param origin Origin slice
     */
    GzipSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final RequestLine line, final Headers headers,
                             final Content body) {
        return connection -> this.origin.response(line, headers, body).send(
            (status, rsheaders, rsbody) -> GzipSlice.gzip(connection, status, rsbody, rsheaders)
        );
    }

    /**
     * Gzip origin response publisher and pass it to connection along with status and headers.
     * @param connection Connection
     * @param stat Response status
     * @param body Origin response body
     * @param headers Origin response headers
     * @return Completable action
     */
    @SuppressWarnings("PMD.CloseResource")
    private static CompletionStage<Void> gzip(final Connection connection, final RsStatus stat,
        final Publisher<ByteBuffer> body, final Headers headers) {
        final CompletionStage<Void> future;
        final CompletableFuture<Void> tmp;
        try (PipedOutputStream resout = new PipedOutputStream();
            PipedInputStream oinput = new PipedInputStream();
            PipedOutputStream tmpout = new PipedOutputStream(oinput)
        ) {
            tmp = CompletableFuture.allOf().thenCompose(
                nothing -> new ReactiveOutputStream(tmpout).write(body, WriteGreed.SYSTEM)
            );
            final PipedInputStream src = new PipedInputStream(resout);
            future = tmp.thenCompose(
                nothing -> connection.accept(
                    stat, headers.copy().add("Content-encoding", "gzip"),
                    new Content.From(new ReactiveInputStream(src).read(Buffers.Standard.K8))
                )
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
        return future;
    }
}
