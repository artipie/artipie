/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Maybe;
import java.io.Closeable;
import java.nio.file.Path;

/**
 * NPM Remote client interface.
 * @since 0.1
 */
public interface NpmRemote extends Closeable {
    /**
     * Loads package from remote repository.
     * @param name Package name
     * @return NPM package or empty
     */
    Maybe<NpmPackage> loadPackage(String name);

    /**
     * Loads asset from remote repository. Typical usage for client:
     * <pre>
     * Path tmp = &lt;create temporary file&gt;
     * NpmAsset asset = remote.loadAsset(asset, tmp);
     * ... consumes asset's data ...
     * Files.delete(tmp);
     * </pre>
     *
     * @param path Asset path
     * @param tmp Temporary file to store asset data
     * @return NpmAsset or empty
     */
    Maybe<NpmAsset> loadAsset(String path, Path tmp);
}
