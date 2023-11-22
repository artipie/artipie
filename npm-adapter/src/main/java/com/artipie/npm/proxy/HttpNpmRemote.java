/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqMethod;
import com.artipie.npm.misc.DateTimeNowStr;
import com.artipie.npm.proxy.json.CachedContent;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import com.jcabi.log.Logger;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Base NPM Remote client implementation. It calls remote NPM repository
 * to download NPM packages and assets. It uses underlying Vertx Web Client inside
 * and works in Rx-way.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class HttpNpmRemote implements NpmRemote {

    /**
     * Origin client slice.
     */
    private final Slice origin;

    /**
     * Ctor.
     * @param origin Client slice
     */
    public HttpNpmRemote(final Slice origin) {
        this.origin = origin;
    }

    @Override
    //@checkstyle ReturnCountCheck (40 lines)
    public Maybe<NpmPackage> loadPackage(final String name) {
        return Maybe.fromFuture(
            this.performRemoteRequest(name).thenCompose(
                pair -> new PublisherAs(pair.getKey()).asciiString().thenApply(
                    str -> new NpmPackage(
                        name,
                        new CachedContent(str, name).value().toString(),
                        HttpNpmRemote.lastModifiedOrNow(pair.getValue()),
                        OffsetDateTime.now()
                    )
                )
            ).toCompletableFuture()
        ).onErrorResumeNext(
            throwable -> {
                Logger.error(
                    HttpNpmRemote.class,
                    "Error occurred when process get package call: %s",
                    throwable.getMessage()
                );
                return Maybe.empty();
            }
        );
    }

    @Override
    //@checkstyle ReturnCountCheck (50 lines)
    public Maybe<NpmAsset> loadAsset(final String path, final Path tmp) {
        return Maybe.fromFuture(
            this.performRemoteRequest(path).thenApply(
                pair -> new NpmAsset(
                    path,
                    pair.getKey(),
                    HttpNpmRemote.lastModifiedOrNow(pair.getValue()),
                    HttpNpmRemote.contentType(pair.getValue())
                )
            )
        ).onErrorResumeNext(
            throwable -> {
                Logger.error(
                    HttpNpmRemote.class,
                    "Error occurred when process get asset call: %s",
                    throwable.getMessage()
                );
                return Maybe.empty();
            }
        );
    }

    @Override
    public void close() {
        //does nothing
    }

    /**
     * Performs request to remote and returns remote body and headers in CompletableFuture.
     * @param name Asset name
     * @return Completable action with content and headers
     */
    private CompletableFuture<Pair<Content, Headers>> performRemoteRequest(final String name) {
        final CompletableFuture<Pair<Content, Headers>> promise = new CompletableFuture<>();
        this.origin.response(
            new RequestLine(RqMethod.GET, String.format("/%s", name)).toString(),
            Headers.EMPTY, Content.EMPTY
        ).send(
            (rsstatus, rsheaders, rsbody) -> {
                final CompletableFuture<Void> term = new CompletableFuture<>();
                if (rsstatus.success()) {
                    final Flowable<ByteBuffer> body = Flowable.fromPublisher(rsbody)
                        .doOnError(term::completeExceptionally)
                        .doOnTerminate(() -> term.complete(null));
                    promise.complete(new ImmutablePair<>(new Content.From(body), rsheaders));
                } else {
                    promise.completeExceptionally(new ArtipieHttpException(rsstatus));
                }
                return term;
            }
        );
        return promise;
    }

    /**
     * Tries to get header {@code Last-Modified} from remote response
     * or returns current time.
     * @param hdrs Remote headers
     * @return Time value.
     */
    private static String lastModifiedOrNow(final Headers hdrs) {
        final RqHeaders hdr = new RqHeaders(hdrs, "Last-Modified");
        String res = new DateTimeNowStr().value();
        if (!hdr.isEmpty()) {
            res = hdr.get(0);
        }
        return res;
    }

    /**
     * Tries to get header {@code ContentType} from remote response
     * or returns {@code application/octet-stream}.
     * @param hdrs Remote headers
     * @return Content type value
     */
    private static String contentType(final Headers hdrs) {
        final RqHeaders hdr = new RqHeaders(hdrs, ContentType.NAME);
        String res = "application/octet-stream";
        if (!hdr.isEmpty()) {
            res = hdr.get(0);
        }
        return res;
    }
}
