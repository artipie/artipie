/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;

/**
 * Connections with {@link Stream.Server} under the hood.
 * @since 0.31
 */
public final class Http3Connection implements Connection {

    /**
     * Http3 server stream.
     */
    private final Stream.Server stream;

    /**
     * Ctor.
     * @param stream Http3 server stream
     */
    public Http3Connection(final Stream.Server stream) {
        this.stream = stream;
    }

    @Override
    public CompletionStage<Void> accept(
        final RsStatus status, final Headers headers, final Content body
    ) {
        final int stat = Integer.parseInt(status.code());
        final MetaData.Response response = new MetaData.Response(
            stat, HttpStatus.getMessage(stat),
            HttpVersion.HTTP_3,
            HttpFields.from(
                StreamSupport.stream(headers.spliterator(), false)
                    .map(item -> new HttpField(item.getKey(), item.getValue()))
                    .toArray(HttpField[]::new)
            )
        );
        final CompletableFuture<Stream> respond =
            this.stream.respond(new HeadersFrame(response, false));
        Flowable.fromPublisher(body)
            .doOnComplete(
                () -> this.stream.data(new DataFrame(ByteBuffer.wrap(new byte[]{}), true))
            ).forEach(
                buffer -> this.stream.data(new DataFrame(buffer, false))
            );
        return respond.thenAccept(ignored -> { });
    }
}
