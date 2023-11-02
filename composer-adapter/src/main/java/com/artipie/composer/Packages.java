/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * PHP Composer packages registry.
 *
 * @since 0.1
 */
public interface Packages {
    /**
     * Add package.
     *
     * @param pack Package.
     * @param version Version in case of absence version in package. If package does not
     *  contain version, this value should be passed as a parameter.
     * @return Updated packages.
     */
    CompletionStage<Packages> add(Package pack, Optional<String> version);

    /**
     * Saves packages registry binary content to storage.
     *
     * @param storage Storage to use for saving.
     * @param key Key to store packages.
     * @return Completion of saving.
     */
    CompletionStage<Void> save(Storage storage, Key key);

    /**
     * Reads packages registry binary content.
     *
     * @return Content.
     */
    CompletionStage<Content> content();
}
