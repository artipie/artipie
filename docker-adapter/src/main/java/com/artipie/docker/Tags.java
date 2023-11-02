/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.asto.Content;

/**
 * Docker repository manifest tags.
 *
 * @since 0.8
 */
public interface Tags {

    /**
     * Read tags in JSON format.
     *
     * @return Tags in JSON format.
     */
    Content json();
}
