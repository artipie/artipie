/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.json.ClientContent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

/**
 * HTTP slice for download package requests.
 */
public final class DownloadPackageSlice implements Slice {
    /**
     * NPM Proxy facade.
     */
    private final NpmProxy npm;

    /**
     * Package path helper.
     */
    private final PackagePath path;

    /**
     * @param npm NPM Proxy facade
     * @param path Package path helper
     */
    public DownloadPackageSlice(final NpmProxy npm, final PackagePath path) {
        this.npm = npm;
        this.path = path;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return this.npm.getPackage(this.path.value(line.uri().getPath()))
            .map(
                pkg -> ResponseBuilder.ok()
                    .header(ContentType.json())
                    .header("Last-Modified", pkg.meta().lastModified())
                    .body(this.clientFormat(pkg.content(), headers).getBytes())
                    .build()
            ).toSingle(ResponseBuilder.notFound().build())
            .to(SingleInterop.get())
            .toCompletableFuture();
    }

    /**
     * Transform internal package format for external clients.
     * @param data Internal package data
     * @param headers Request headers
     * @return External client package
     */
    private String clientFormat(final String data,
        final Iterable<Header> headers) {
        final String host = StreamSupport.stream(headers.spliterator(), false)
            .filter(e -> "Host".equalsIgnoreCase(e.getKey()))
            .findAny().orElseThrow(
                () -> new RuntimeException("Could not find Host header in request")
            ).getValue();
        return new ClientContent(data, this.assetPrefix(host)).value().toString();
    }

    /**
     * Generates asset base reference.
     * @param host External host
     * @return Asset base reference
     */
    private String assetPrefix(final String host) {
        final String result;
        if (StringUtils.isEmpty(this.path.prefix())) {
            result = String.format("http://%s", host);
        } else {
            result = String.format("http://%s/%s", host, this.path.prefix());
        }
        return result;
    }
}
