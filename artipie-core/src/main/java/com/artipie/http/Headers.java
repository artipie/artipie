/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.headers.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * HTTP request headers.
 */
public class Headers implements Iterable<Header> {

    public static Headers EMPTY = new Headers(Collections.emptyList());

    public static Headers from(String name, String value) {
        return from(new Header(name, value));
    }

    public static Headers from(Header header) {
        List<Header> list = new ArrayList<>();
        list.add(header);
        return new Headers(list);
    }

    public static Headers from(Iterable<Map.Entry<String, String>> multiMap) {
        return new Headers(
            StreamSupport.stream(multiMap.spliterator(), false)
                .map(Header::new)
                .toList()
        );
    }

    @SafeVarargs
    public static Headers from(Map.Entry<String, String>... entries) {
        return new Headers(Arrays.stream(entries).map(Header::new).toList());
    }

    private final List<Header> headers;

    public Headers() {
        this.headers = new ArrayList<>();
    }

    public Headers(List<Header> headers) {
        this.headers = headers;
    }

    public Headers add(String name, String value) {
        headers.add(new Header(name, value));
        return this;
    }

    public Headers add(Header header, boolean overwrite) {
        if (overwrite) {
            headers.removeIf(h -> h.getKey().equals(header.getKey()));
        }
        headers.add(header);
        return this;
    }

    public Headers add(Header header) {
        headers.add(header);
        return this;
    }

    public Headers add(Map.Entry<String, String> entry) {
        return add(entry.getKey(), entry.getValue());
    }

    public Headers addAll(Headers src) {
        headers.addAll(src.headers);
        return this;
    }

    public Headers copy() {
        return new Headers(new ArrayList<>(headers));
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    public List<String> values(String name) {
        return headers.stream()
            .filter(h -> h.getKey().equalsIgnoreCase(name))
            .map(Header::getValue)
            .toList();
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    public Stream<Header> stream() {
        return headers.stream();
    }

    public List<Header> asList() {
        return new ArrayList<>(headers);
    }

    public String asString() {
        return headers.stream()
            .map(h -> h.getKey() + '=' + h.getValue())
            .collect(Collectors.joining(";"));
    }

    @Override
    public String toString() {
        return "Headers{" +
            "headers=" + headers +
            '}';
    }
}
