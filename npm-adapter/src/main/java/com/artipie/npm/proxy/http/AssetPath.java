/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Asset path helper. Artipie maps concrete repositories on the path prefixes in the URL.
 * This class provides the way to match asset requests with prefixes correctly.
 * Also, it allows to get relative asset path for using with the Storage instances.
 * @since 0.1
 */
public final class AssetPath extends NpmPath {
    /**
     * Ctor.
     * @param prefix Base prefix path
     */
    public AssetPath(final String prefix) {
        super(prefix);
    }

    @Override
    public Pattern pattern() {
        final Pattern result;
        if (StringUtils.isEmpty(this.prefix())) {
            result = Pattern.compile("^/(.+/-/.+)$");
        } else {
            result = Pattern.compile(
                String.format("^/%1$s/(.+/-/.+)$", Pattern.quote(this.prefix()))
            );
        }
        return result;
    }
}
