/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLinePrefix;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * SliceIndex returns formatted html output with index of repository packages.
 *
 * @since 0.2
 */
final class SliceIndex implements Slice {

    /**
     * Artipie artifacts storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    SliceIndex(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publisher
    ) {
        final Key rqkey = new KeyFromPath(line.uri().toString());
        final String prefix = new RequestLinePrefix(rqkey.string(), headers).get();
        return new AsyncResponse(
            SingleInterop.fromFuture(this.storage.list(rqkey))
                .flatMapPublisher(Flowable::fromIterable)
                .flatMapSingle(
                    key -> Single.fromFuture(
                        this.storage.value(key).thenCompose(
                            value -> new ContentDigest(value, Digests.SHA256).hex()
                        ).thenApply(
                            hex -> String.format(
                                "<a href=\"%s#sha256=%s\">%s</a><br/>",
                                String.format("%s/%s", prefix, key.string()),
                                hex,
                                new KeyLastPart(key).get()
                            )
                        )
                    )
                )
                .collect(StringBuilder::new, StringBuilder::append)
                .<Response>map(
                    resp -> new RsWithBody(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.OK),
                            new ContentType("text/html")
                        ),
                        String.format(
                            "<!DOCTYPE html>\n<html>\n  </body>\n%s\n</body>\n</html>",
                            resp.toString()
                        ),
                        StandardCharsets.UTF_8
                    )
                )
        );
    }

}
