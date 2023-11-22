/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base path helper class NPM Proxy.
 * @since 0.1
 */
public abstract class NpmPath {
    /**
     * Base path prefix.
     */
    private final String base;

    /**
     * Ctor.
     * @param prefix Base path prefix
     */
    public NpmPath(final String prefix) {
        this.base = prefix;
    }

    /**
     * Gets relative path from absolute.
     * @param abspath Absolute path
     * @return Relative path
     */
    public final String value(final String abspath) {
        final Matcher matcher = this.pattern().matcher(abspath);
        if (matcher.matches()) {
            final String path = matcher.group(1);
            Logger.debug(this, "Determined path is: %s", path);
            return path;
        } else {
            throw new ArtipieException(
                new IllegalArgumentException(
                    String.format(
                        "Given absolute path [%s] does not match with pattern [%s]",
                        abspath,
                        this.pattern().toString()
                    )
                )
            );
        }
    }

    /**
     * Gets base path prefix.
     * @return Bas path prefix
     */
    public final String prefix() {
        return this.base;
    }

    /**
     * Gets pattern to match handled paths.
     * @return Pattern to match handled paths
     */
    public abstract Pattern pattern();
}
