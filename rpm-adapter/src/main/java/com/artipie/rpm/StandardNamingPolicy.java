/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Standard naming policies.
 * @since 0.6
 */
public enum StandardNamingPolicy implements NamingPolicy {
    /**
     * Plain simple names.
     */
    PLAIN(new NamingPolicy() {

        @Override
        public String name(final String source, final Path content) {
            return source;
        }

        @Override
        public String fullName(final XmlPackage source, final String prefix) {
            return String.format("repodata/%s.xml.gz", source.lowercase());
        }
    }),
    /**
     * Add SHA1 prefixes to names.
     */
    SHA1(new HashPrefixed(Digest.SHA1)),
    /**
     * Add SHA256 prefixes to names.
     */
    SHA256(new HashPrefixed(Digest.SHA256));

    /**
     * Origin policy.
     */
    private final NamingPolicy origin;

    /**
     * Enum ctor.
     * @param origin Origin policy
     */
    StandardNamingPolicy(final NamingPolicy origin) {
        this.origin = origin;
    }

    @Override
    public String name(final String source, final Path content) throws IOException {
        return this.origin.name(source, content);
    }

    @Override
    public String fullName(final XmlPackage source, final String prefix) {
        return this.origin.fullName(source, prefix);
    }
}
