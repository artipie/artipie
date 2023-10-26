/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RqHeaders;

/**
 * Docker-Content-Digest header.
 * See <a href="https://docs.docker.com/registry/spec/api/#blob-upload#content-digests">Content Digests</a>.
 *
 * @since 0.2
 */
public final class DigestHeader extends Header.Wrap {

    /**
     * Header name.
     */
    private static final String NAME = "Docker-Content-Digest";

    /**
     * Ctor.
     *
     * @param digest Digest value.
     */
    public DigestHeader(final Digest digest) {
        this(digest.string());
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public DigestHeader(final Headers headers) {
        this(new RqHeaders.Single(headers, DigestHeader.NAME).asString());
    }

    /**
     * Ctor.
     *
     * @param digest Digest value.
     */
    private DigestHeader(final String digest) {
        super(new Header(DigestHeader.NAME, digest));
    }

    /**
     * Read header as numeric value.
     *
     * @return Header value.
     */
    public Digest value() {
        return new Digest.FromString(this.getValue());
    }
}
