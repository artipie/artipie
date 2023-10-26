/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Completable;
import io.reactivex.Maybe;

/**
 * NPM Proxy storage interface.
 * @since 0.1
 */
public interface NpmProxyStorage {
    /**
     * Persist NPM Package.
     * @param pkg Package to persist
     * @return Completion or error signal
     */
    Completable save(NpmPackage pkg);

    /**
     * Persist NPM Asset.
     * @param asset Asset to persist
     * @return Completion or error signal
     */
    Completable save(NpmAsset asset);

    /**
     * Retrieve NPM package by name.
     * @param name Package name
     * @return NPM package or empty
     */
    Maybe<NpmPackage> getPackage(String name);

    /**
     * Retrieve NPM asset by path.
     * @param path Asset path
     * @return NPM asset or empty
     */
    Maybe<NpmAsset> getAsset(String path);
}
