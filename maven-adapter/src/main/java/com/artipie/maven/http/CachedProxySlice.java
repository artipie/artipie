/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.DigestVerification;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.ext.Digests;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.jcabi.log.Logger;
import io.reactivex.Flowable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Maven proxy slice with cache support.
 */
final class CachedProxySlice implements Slice {

    /**
     * Checksum header pattern.
     */
    private static final Pattern CHECKSUM_PATTERN =
        Pattern.compile("x-checksum-(sha1|sha256|sha512|md5)", Pattern.CASE_INSENSITIVE);

    /**
     * Translation of checksum headers to digest algorithms.
     */
    private static final Map<String, String> DIGEST_NAMES = Map.of(
        "sha1", "SHA-1",
        "sha256", "SHA-256",
        "sha512", "SHA-512",
        "md5", "MD5"
    );

    /**
     * Origin slice.
     */
    private final Slice client;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Proxy artifact events.
     */
    private final Optional<Queue<ProxyArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Wraps origin slice with caching layer.
     * @param client Client slice
     * @param cache Cache
     * @param events Artifact events
     * @param rname Repository name
     */
    CachedProxySlice(final Slice client, final Cache cache,
        final Optional<Queue<ProxyArtifactEvent>> events, final String rname) {
        this.client = client;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Response response(RequestLine line, Headers headers,
                             Content body) {
        final Key key = new KeyFromPath(line.uri().getPath());
        final AtomicReference<Headers> rshdr = new AtomicReference<>(Headers.EMPTY);
        return new AsyncResponse(
            new RepoHead(this.client)
                .head(line.uri().getPath()).thenCompose(
                    head -> this.cache.load(
                        key,
                        new Remote.WithErrorHandling(
                            () -> {
                                final CompletableFuture<Optional<? extends Content>> promise =
                                    new CompletableFuture<>();
                                this.client.response(line, Headers.EMPTY, Content.EMPTY).send(
                                    (rsstatus, rsheaders, rsbody) -> {
                                        final CompletableFuture<Void> term =
                                            new CompletableFuture<>();
                                        if (rsstatus.success()) {
                                            final Flowable<ByteBuffer> res =
                                                Flowable.fromPublisher(rsbody)
                                                .doOnError(term::completeExceptionally)
                                                .doOnTerminate(() -> term.complete(null));
                                            this.addEventToQueue(key);
                                            promise.complete(Optional.of(new Content.From(res)));
                                        } else {
                                            promise.complete(Optional.empty());
                                        }
                                        rshdr.set(rsheaders);
                                        return term;
                                    }
                                );
                                return promise;
                            }
                        ),
                        new CacheControl.All(
                            StreamSupport.stream(
                                head.orElse(Headers.EMPTY).spliterator(),
                                false
                            ).map(Header::new)
                            .map(CachedProxySlice::checksumControl)
                            .collect(Collectors.toUnmodifiableList())
                        )
                    ).handle(
                        (content, throwable) -> {
                            if (throwable == null && content.isPresent()) {
                                return BaseResponse.ok()
                                    .headers(rshdr.get())
                                    .body(content.get());
                            }
                            if (throwable != null) {
                                Logger.error(this, throwable.getMessage());
                            }
                            return BaseResponse.notFound();
                        }
                    )
            )
        );
    }

    /**
     * Adds artifact data to events queue, if this queue is present.
     * Note, that
     * - checksums, javadoc and sources archives are excluded
     * - event key contains package name and version, for example 'com/artipie/asto/1.5'
     * It is possible, that the same package will be added to the queue twice
     * (as one maven package can contain pom, jar, war etc. at the same time), but will not
     * be duplicated as {@link ProxyArtifactEvent} with the same package key are considered as
     * equal.
     * @param key Artifact key
     */
    private void addEventToQueue(final Key key) {
        if (this.events.isPresent()) {
            final Matcher matcher = MavenSlice.ARTIFACT.matcher(key.string());
            if (matcher.matches()) {
                this.events.get().add(
                    new ProxyArtifactEvent(new Key.From(matcher.group("pkg")), this.rname)
                );
            }
        }
    }

    /**
     * Checksum cache control verification.
     * @param header Checksum header
     * @return Cache control with digest
     */
    private static CacheControl checksumControl(final Header header) {
        final Matcher matcher = CachedProxySlice.CHECKSUM_PATTERN.matcher(header.getKey());
        final CacheControl res;
        if (matcher.matches()) {
            try {
                res = new DigestVerification(
                    new Digests.FromString(
                        CachedProxySlice.DIGEST_NAMES.get(
                            matcher.group(1).toLowerCase(Locale.US)
                        )
                    ).get(),
                    Hex.decodeHex(header.getValue().toCharArray())
                );
            } catch (final DecoderException err) {
                throw new IllegalStateException("Invalid digest hex", err);
            }
        } else {
            res = CacheControl.Standard.ALWAYS;
        }
        return res;
    }
}
