/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;

/**
 * Docker-Content-Digest header.
 * See <a href="https://docs.docker.com/registry/spec/api/#blob-upload#content-digests">Content Digests</a>.
 */
public final class DigestHeader extends Header {

    /**
     * Header name.
     */
    private static final String NAME = "Docker-Content-Digest";

    /**
     * @param digest Digest value.
     */
    public DigestHeader(Digest digest) {
        this(digest.string());
    }

    /**
     * @param headers Headers to extract header from.
     */
    public DigestHeader(Headers headers) {
        this(headers.single(DigestHeader.NAME).getValue());
    }

    /**
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
