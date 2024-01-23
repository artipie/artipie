/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.pypi.NormalizedProjectName;
import com.artipie.scheduling.ProxyArtifactEvent;
import io.reactivex.Flowable;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.reactivestreams.Publisher;

/**
 * Slice that proxies request with given request line and empty headers and body,
 * caches and returns response from remote.
 * @since 0.7
 */
final class ProxySlice implements Slice {

    /**
     * Python artifacts formats.
     */
    private static final String FORMATS = ".*\\.(whl|tar\\.gz|zip|tar\\.bz2|tar\\.Z|tar|egg)";

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Proxy artifacts events.
     */
    private final Optional<Queue<ProxyArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Ctor.
     * @param origin Origin
     * @param cache Cache
     * @param events Artifact events queue
     * @param rname Repository name
         */
    ProxySlice(final Slice origin, final Cache cache,
        final Optional<Queue<ProxyArtifactEvent>> events,
        final String rname) {
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> ignored,
        final Publisher<ByteBuffer> pub
    ) {
        final AtomicReference<Headers> headers = new AtomicReference<>();
        final Key key = ProxySlice.keyFromPath(line);
        return new AsyncResponse(
            this.cache.load(
                key,
                new Remote.WithErrorHandling(
                    () -> {
                        final CompletableFuture<Optional<? extends Content>> promise =
                            new CompletableFuture<>();
                        this.origin.response(line, Headers.EMPTY, Content.EMPTY).send(
                            (rsstatus, rsheaders, rsbody) -> {
                                final CompletableFuture<Void> term = new CompletableFuture<>();
                                headers.set(rsheaders);
                                if (rsstatus.success()) {
                                    final Flowable<ByteBuffer> body = Flowable.fromPublisher(rsbody)
                                        .doOnError(term::completeExceptionally)
                                        .doOnTerminate(() -> term.complete(null));
                                    promise.complete(Optional.of(new Content.From(body)));
                                    this.events.ifPresent(
                                        queue -> queue.add(new ProxyArtifactEvent(key, this.rname))
                                    );
                                } else {
                                    promise.complete(Optional.empty());
                                }
                                return term;
                            }
                        );
                        return promise;
                    }
                ),
                CacheControl.Standard.ALWAYS
            ).handle(
                (content, throwable) -> {
                    final CompletableFuture<Response> result = new CompletableFuture<>();
                    if (throwable == null && content.isPresent()) {
                        result.complete(
                            new RsFull(
                                RsStatus.OK,
                                new Headers.From(ProxySlice.contentType(headers.get(), line)),
                                content.get()
                            )
                        );
                    } else {
                        result.complete(new RsWithStatus(RsStatus.NOT_FOUND));
                    }
                    return result;
                }
            ).thenCompose(Function.identity())
        );
    }

    /**
     * Obtains content-type from remote's headers or trays to guess it by request line.
     * @param headers Header
     * @param line Request line
     * @return Cleaned up headers.
     */
    private static Header contentType(final Headers headers, final String line) {
        final String name = "content-type";
        return Optional.ofNullable(headers).flatMap(
            hdrs -> StreamSupport.stream(hdrs.spliterator(), false)
                .filter(header -> header.getKey().equalsIgnoreCase(name)).findFirst()
                .map(Header::new)
            ).orElseGet(
                () -> {
                    Header res = new Header(name, "text/html");
                    final String ext = new RequestLineFrom(line).uri().toString();
                    if (ext.matches(ProxySlice.FORMATS)) {
                        res = new Header(
                            name,
                            Optional.ofNullable(URLConnection.guessContentTypeFromName(ext))
                                .orElse("*")
                        );
                    }
                    return res;
                }
            );
    }

    /**
     * Obtains key from request line with names normalization.
     * @param line Request line
     * @return Instance of {@link Key}.
     */
    private static Key keyFromPath(final String line) {
        final URI uri = new RequestLineFrom(line).uri();
        Key res = new KeyFromPath(uri.getPath());
        if (!uri.toString().matches(ProxySlice.FORMATS)) {
            final String last = new KeyLastPart(res).get();
            res = new Key.From(
                res.string().replaceAll(
                    String.format("%s$", last), new NormalizedProjectName.Simple(last).value()
                )
            );
        }
        return res;
    }
}
