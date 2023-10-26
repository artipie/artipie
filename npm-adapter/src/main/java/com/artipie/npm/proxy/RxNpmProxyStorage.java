/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.rx.RxStorage;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;

/**
 * Base NPM Proxy storage implementation. It encapsulates storage format details
 * and allows to handle both primary data and metadata files within one calls.
 * It uses underlying RxStorage and works in Rx-way.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RxNpmProxyStorage implements NpmProxyStorage {
    /**
     * Underlying storage.
     */
    private final RxStorage storage;

    /**
     * Ctor.
     * @param storage Underlying storage
     */
    public RxNpmProxyStorage(final RxStorage storage) {
        this.storage = storage;
    }

    @Override
    public Completable save(final NpmPackage pkg) {
        final Key key = new Key.From(pkg.name(), "meta.json");
        return Completable.concatArray(
            this.storage.save(
                key,
                new Content.From(pkg.content().getBytes(StandardCharsets.UTF_8))
            ),
            this.storage.save(
                new Key.From(pkg.name(), "meta.meta"),
                new Content.From(
                    pkg.meta().json().encode().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    @Override
    public Completable save(final NpmAsset asset) {
        final Key key = new Key.From(asset.path());
        return Completable.concatArray(
            this.storage.save(
                key,
                new Content.From(asset.dataPublisher())
            ),
            this.storage.save(
                new Key.From(
                    String.format("%s.meta", asset.path())
                ),
                new Content.From(
                    asset.meta().json().encode().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    @Override
    // @checkstyle ReturnCountCheck (15 lines)
    public Maybe<NpmPackage> getPackage(final String name) {
        return this.storage.exists(new Key.From(name, "meta.json"))
            .flatMapMaybe(
                exists -> {
                    if (exists) {
                        return this.readPackage(name).toMaybe();
                    } else {
                        return Maybe.empty();
                    }
                }
            );
    }

    @Override
    // @checkstyle ReturnCountCheck (15 lines)
    public Maybe<NpmAsset> getAsset(final String path) {
        return this.storage.exists(new Key.From(path))
            .flatMapMaybe(
                exists -> {
                    if (exists) {
                        return this.readAsset(path).toMaybe();
                    } else {
                        return Maybe.empty();
                    }
                }
            );
    }

    /**
     * Read NPM package from storage.
     * @param name Package name
     * @return NPM package
     */
    private Single<NpmPackage> readPackage(final String name) {
        return this.storage.value(new Key.From(name, "meta.json"))
            .map(PublisherAs::new)
            .map(PublisherAs::bytes)
            .flatMap(SingleInterop::fromFuture)
            .zipWith(
                this.storage.value(new Key.From(name, "meta.meta"))
                    .map(PublisherAs::new)
                    .map(PublisherAs::bytes)
                    .flatMap(SingleInterop::fromFuture)
                    .map(metadata -> new String(metadata, StandardCharsets.UTF_8))
                    .map(JsonObject::new),
                (content, metadata) ->
                    new NpmPackage(
                        name,
                        new String(content, StandardCharsets.UTF_8),
                        new NpmPackage.Metadata(metadata)
                    )
                );
    }

    /**
     * Read NPM Asset from storage.
     * @param path Asset path
     * @return NPM asset
     */
    private Single<NpmAsset> readAsset(final String path) {
        return this.storage.value(new Key.From(path))
            .zipWith(
                this.storage.value(new Key.From(String.format("%s.meta", path)))
                    .map(PublisherAs::new)
                    .map(PublisherAs::bytes)
                    .flatMap(SingleInterop::fromFuture)
                    .map(metadata -> new String(metadata, StandardCharsets.UTF_8))
                    .map(JsonObject::new),
                (content, metadata) ->
                    new NpmAsset(
                        path,
                        content,
                        new NpmAsset.Metadata(metadata)
                    )
            );
    }

}
