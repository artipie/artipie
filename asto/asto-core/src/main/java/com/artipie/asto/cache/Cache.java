/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Generic reactive cache which returns cached content by key of exist or loads from remote and
 * cache if doesn't exit.
 *
 * @since 0.24
 */
public interface Cache {

    /**
     * No cache, just load remote resource.
     */
    Cache NOP = (key, remote, ctl) -> remote.get();

    /**
     * Try to load content from cache or fallback to remote publisher if cached key doesn't exist.
     * When loading remote item, the cache may save its content to the cache storage.
     * @param key Cached item key
     * @param remote Remote source
     * @param control Cache control
     * @return Content for key
     */
    CompletionStage<Optional<? extends Content>> load(
        Key key, Remote remote, CacheControl control
    );
}
