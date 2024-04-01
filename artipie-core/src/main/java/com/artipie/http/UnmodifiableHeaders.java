/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.headers.Header;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unmodifiable list of HTTP request headers.
 */
public class UnmodifiableHeaders extends Headers {

    UnmodifiableHeaders(List<Header> headers) {
        super(Collections.unmodifiableList(headers));
    }

    @Override
    public Headers add(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Headers add(Header header, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Headers add(Header header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Headers add(Map.Entry<String, String> entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Headers addAll(Headers src) {
        throw new UnsupportedOperationException();
    }
}
