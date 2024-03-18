/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.JsonPackages;
import com.artipie.composer.Packages;
import com.artipie.composer.Repository;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.jcabi.log.Logger;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Composer proxy slice with cache support.
 * @since 0.4
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
final class CachedProxySlice implements Slice {
    /**
     * Remote slice.
     */
    private final Slice remote;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Proxy slice without cache.
     * @param remote Remote slice
     * @param repo Repository
     */
    CachedProxySlice(final Slice remote, final Repository repo) {
        this(remote, repo, Cache.NOP);
    }

    /**
     * Ctor.
     * @param remote Remote slice
     * @param repo Repository
     * @param cache Cache
     */
    CachedProxySlice(final Slice remote, final Repository repo, final Cache cache) {
        this.remote = remote;
        this.cache = cache;
        this.repo = repo;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body
    ) {
        final String name = line
            .uri().getPath().replaceAll("^/p2?/", "")
            .replaceAll("~.*", "")
            .replaceAll("\\^.*", "")
            .replaceAll(".json$", "");
        return new AsyncResponse(
            this.cache.load(
                new Key.From(name),
                new Remote.WithErrorHandling(
                    () -> this.repo.packages().thenApply(
                        pckgs -> pckgs.orElse(new JsonPackages())
                    ).thenCompose(Packages::content)
                        .thenCombine(
                            this.packageFromRemote(line),
                            (lcl, rmt) -> new MergePackage.WithRemote(name, lcl).merge(rmt)
                        ).thenCompose(Function.identity())
                        .thenApply(Function.identity())
                ),
                new CacheTimeControl(this.repo.storage())
            ).handle(
                (pkgs, throwable) -> {
                    final Response res;
                    if (throwable == null && pkgs.isPresent()) {
                        res = new RsWithBody(StandardRs.OK, pkgs.get());
                    } else {
                        Logger.warn(this, "Failed to read cached item: %[exception]s", throwable);
                        res = StandardRs.NOT_FOUND;
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Obtains info about package from remote.
     * @param line The request line (usually like this `GET /p2/vendor/package.json HTTP_1_1`)
     * @return Content from respond of remote. If there were some errors,
     *  empty will be returned.
     */
    private CompletionStage<Optional<? extends Content>> packageFromRemote(final RequestLine line) {
        return new Remote.WithErrorHandling(
            () -> {
                final CompletableFuture<Optional<? extends Content>> promise;
                promise = new CompletableFuture<>();
                this.remote.response(line, Headers.EMPTY, Content.EMPTY).send(
                    (rsstatus, rsheaders, rsbody) -> {
                        if (rsstatus.success()) {
                            promise.complete(Optional.of(new Content.From(rsbody)));
                        } else {
                            promise.complete(Optional.empty());
                        }
                        return CompletableFuture.allOf();
                    }
                );
                return promise;
            }
        ).get();
    }
}
