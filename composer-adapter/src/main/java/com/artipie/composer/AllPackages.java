/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Key;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Key for all packages value.
 *
 * @since 0.1
 */
public final class AllPackages implements Key {

    @Override
    public String string() {
        return "packages.json";
    }

    @Override
    public Optional<Key> parent() {
        return Optional.empty();
    }

    @Override
    public List<String> parts() {
        return Collections.singletonList(this.string());
    }
}
