/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.client.ClientSlices;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.StoragesCache;

/**
 * Slice handles repository requests extracting repository name from URI path.
 *
 * @since 0.11
 */
public final class AllRepositoriesSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param http HTTP client
     * @param settings Artipie settings
     * @param cache Storages cache
     */
    public AllRepositoriesSlice(
        final ClientSlices http,
        final Settings settings,
        final StoragesCache cache
    ) {
        super(new DockerRoutingSlice(settings, new SliceByPath(http, settings, cache)));
    }
}
