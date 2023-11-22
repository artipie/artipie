/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Content;
import com.artipie.asto.test.TestResource;

/**
 * Newton.Json package resource.
 *
 * @since 0.1
 */
public final class NewtonJsonResource {

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param name Resource name.
     */
    public NewtonJsonResource(final String name) {
        this.name = name;
    }

    /**
     * Reads binary data.
     *
     * @return Binary data.
     */
    public Content content() {
        return new Content.From(this.bytes());
    }

    /**
     * Reads binary data.
     *
     * @return Binary data.
     */
    public byte[] bytes() {
        return new TestResource(String.format("newtonsoft.json/12.0.3/%s", this.name)).asBytes();
    }
}
