/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentFileName;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice to download repodata.json. If the repodata item does not exists in storage, empty
 * json is returned.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class DownloadRepodataSlice implements Slice {

    /**
     * Request path pattern.
     */
    private static final Pattern RQ_PATH = Pattern.compile(".*/((.+)/(current_)?repodata\\.json)");

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public DownloadRepodataSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            CompletableFuture
                .supplyAsync(() -> new RequestLineFrom(line).uri().getPath())
                .thenCompose(
                    path -> {
                        final Matcher matcher = DownloadRepodataSlice.RQ_PATH.matcher(path);
                        final CompletionStage<Response> res;
                        if (matcher.matches()) {
                            final Key key = new Key.From(matcher.group(1));
                            res = this.asto.exists(key).thenCompose(
                                exist -> {
                                    final CompletionStage<Content> content;
                                    if (exist) {
                                        content = this.asto.value(key);
                                    } else {
                                        content = CompletableFuture.completedFuture(
                                            new Content.From(
                                                Json.createObjectBuilder().add(
                                                    "info", Json.createObjectBuilder()
                                                        .add("subdir", matcher.group(2))
                                                ).build().toString()
                                                    .getBytes(StandardCharsets.US_ASCII)
                                            )
                                        );
                                    }
                                    return content;
                                }
                            ).thenApply(
                                content -> new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new ContentFileName(new KeyLastPart(key).get())
                                    ),
                                    content
                                )
                            );
                        } else {
                            res = CompletableFuture
                                .completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST));
                        }
                        return res;
                    }
                )
        );
    }
}
