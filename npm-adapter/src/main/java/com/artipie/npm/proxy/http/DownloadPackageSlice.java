/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.json.ClientContent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

/**
 * HTTP slice for download package requests.
 * @since 0.1
 * @checkstyle ReturnCountCheck (200 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
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
     * Ctor.
     *
     * @param npm NPM Proxy facade
     * @param path Package path helper
     */
    public DownloadPackageSlice(final NpmProxy npm, final PackagePath path) {
        this.npm = npm;
        this.path = path;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.npm.getPackage(this.path.value(new RequestLineFrom(line).uri().getPath()))
                .map(
                    pkg -> (Response) new RsFull(
                        RsStatus.OK,
                        new Headers.From(
                            new Header("Content-Type", "application/json"),
                            new Header("Last-Modified", pkg.meta().lastModified())
                        ),
                        new Content.From(
                            this.clientFormat(pkg.content(), headers).getBytes()
                        )
                    )
                ).toSingle(new RsNotFound())
                .to(SingleInterop.get())
        );
    }

    /**
     * Transform internal package format for external clients.
     * @param data Internal package data
     * @param headers Request headers
     * @return External client package
     */
    private String clientFormat(final String data,
        final Iterable<Map.Entry<String, String>> headers) {
        final String host = StreamSupport.stream(headers.spliterator(), false)
            .filter(e -> e.getKey().equalsIgnoreCase("Host"))
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
