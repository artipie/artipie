/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * URI query parameters. See <a href="https://tools.ietf.org/html/rfc3986#section-3.4">RFC</a>.
 */
public final class RqParams {

    private final List<NameValuePair> params;

    /**
     * @param uri Request URI.
     */
    public RqParams(URI uri) {
        params = new URIBuilder(uri).getQueryParams();
    }

    /**
     * Get value for parameter value by name.
     * Empty {@link Optional} is returned if parameter not found.
     * First value is returned if multiple parameters with same name present in the query.
     *
     * @param name Parameter name.
     * @return Parameter value.
     */
    public Optional<String> value(String name) {
        return params.stream()
            .filter(p -> Objects.equals(name, p.getName()))
            .map(NameValuePair::getValue)
            .findFirst();
    }

    /**
     * Get values for parameter value by name.
     * Empty {@link List} is returned if parameter not found.
     * Return List with all founded values if parameters with same name present in query
     *
     * @param name Parameter name.
     * @return List of Parameter values
     */
    public List<String> values(String name) {
        return params.stream()
            .filter(p -> Objects.equals(name, p.getName()))
            .map(NameValuePair::getValue)
            .toList();
    }
}
