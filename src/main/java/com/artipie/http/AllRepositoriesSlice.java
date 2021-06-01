/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.Settings;

/**
 * Slice handles repository requests extracting repository name from URI path.
 *
 * @since 0.11
 */
public final class AllRepositoriesSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param settings Artipie settings.
     */
    public AllRepositoriesSlice(final Settings settings) {
        super(new DockerRoutingSlice(settings, new SliceByPath(settings)));
    }
}
