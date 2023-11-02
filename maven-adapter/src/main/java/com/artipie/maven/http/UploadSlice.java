/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * This slice accepts PUT requests with jars/poms etc (any files except for metadata and
 * metadata checksums) and saves received data to the temp location.
 * @since 0.8
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class UploadSlice implements Slice {

    /**
     * Temp storage key.
     */
    static final Key TEMP = new Key.From(".upload");

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public UploadSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.asto.save(
                new Key.From(
                    UploadSlice.TEMP,
                    new KeyFromPath(new RequestLineFrom(line).uri().getPath())
                ),
                new ContentWithSize(body, headers)
            ).thenApply(nothing -> new RsWithStatus(RsStatus.CREATED))
        );
    }
}
