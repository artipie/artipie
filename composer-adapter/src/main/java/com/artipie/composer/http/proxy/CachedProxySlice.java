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
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.jcabi.log.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Composer proxy slice with cache support.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
final class CachedProxySlice implements Slice {

    private final Slice remote;
    private final Cache cache;
    private final Repository repo;

    /**
     * @param remote Remote slice
     * @param repo Repository
     * @param cache Cache
     */
    CachedProxySlice(Slice remote, Repository repo, Cache cache) {
        this.remote = remote;
        this.cache = cache;
        this.repo = repo;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
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
                    if (throwable == null && pkgs.isPresent()) {
                        return ResponseBuilder.ok().body(pkgs.get()).build();
                    }
                    Logger.warn(this, "Failed to read cached item: %[exception]s", throwable);
                    return ResponseBuilder.notFound().build();
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
