/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Maybe;
import java.io.IOException;
import java.net.URI;

/**
 * NPM Proxy.
 * @since 0.1
 */
public class NpmProxy {

    /**
     * The storage.
     */
    private final NpmProxyStorage storage;

    /**
     * Remote repository client.
     */
    private final NpmRemote remote;

    /**
     * Ctor.
     * @param remote Uri remote
     * @param storage Adapter storage
     * @param client Client slices
     */
    public NpmProxy(final URI remote, final Storage storage, final ClientSlices client) {
        this(
            new RxNpmProxyStorage(new RxStorageWrapper(storage)),
            new HttpNpmRemote(new UriClientSlice(client, remote))
        );
    }

    /**
     * Ctor.
     * @param storage Adapter storage
     * @param client Client slice
     */
    public NpmProxy(final Storage storage, final Slice client) {
        this(
            new RxNpmProxyStorage(new RxStorageWrapper(storage)),
            new HttpNpmRemote(client)
        );
    }

    /**
     * Default-scoped ctor (for tests).
     * @param storage NPM storage
     * @param remote Remote repository client
     */
    NpmProxy(final NpmProxyStorage storage, final NpmRemote remote) {
        this.storage = storage;
        this.remote = remote;
    }

    /**
     * Retrieve package metadata.
     * @param name Package name
     * @return Package metadata (cached or downloaded from remote repository)
     */
    public Maybe<NpmPackage> getPackage(final String name) {
        return this.storage.getPackage(name).flatMap(
            pkg -> this.remotePackage(name).switchIfEmpty(Maybe.just(pkg))
        ).switchIfEmpty(Maybe.defer(() -> this.remotePackage(name)));
    }

    /**
     * Retrieve asset.
     * @param path Asset path
     * @return Asset data (cached or downloaded from remote repository)
     */
    public Maybe<NpmAsset> getAsset(final String path) {
        return this.storage.getAsset(path).switchIfEmpty(
            Maybe.defer(
                () -> this.remote.loadAsset(path, null).flatMap(
                    asset -> this.storage.save(asset)
                        .andThen(Maybe.defer(() -> this.storage.getAsset(path)))
                )
            )
        );
    }

    /**
     * Close NPM Proxy adapter and underlying remote client.
     * @throws IOException when underlying remote client fails to close
     */
    public void close() throws IOException {
        this.remote.close();
    }

    /**
     * Get package from remote repository and save it to storage.
     * @param name Package name
     * @return Npm Package
     */
    private Maybe<NpmPackage> remotePackage(final String name) {
        final Maybe<NpmPackage> res;
        final Maybe<NpmPackage> pckg = this.remote.loadPackage(name);
        if (pckg == null) {
            res = Maybe.empty();
        } else {
            res = pckg.flatMap(
                pkg -> this.storage.save(pkg).andThen(Maybe.just(pkg))
            );
        }
        return res;
    }
}
