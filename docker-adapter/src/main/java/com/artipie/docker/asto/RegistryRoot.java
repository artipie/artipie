/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;

/**
 * Docker registry root key.
 * @since 0.1
 */
public final class RegistryRoot extends Key.Wrap {

    /**
     * Registry root key.
     */
    public static final RegistryRoot V2 = new RegistryRoot("v2");

    /**
     * Ctor.
     * @param version Registry version
     */
    private RegistryRoot(final String version) {
        super(new Key.From("docker", "registry", version));
    }
}
