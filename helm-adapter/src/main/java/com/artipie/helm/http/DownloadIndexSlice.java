/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.BaseResponse;
import com.artipie.http.slice.KeyFromPath;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Download index file endpoint. Return index file with urls that are
 * based on requested URL.
 */
final class DownloadIndexSlice implements Slice {
    /**
     * Endpoint request line pattern.
     */
    static final Pattern PTRN = Pattern.compile(".*index.yaml$");

    /**
     * Base URL.
     */
    private final URL base;

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param base Base URL
     * @param storage Abstract storage
     */
    DownloadIndexSlice(final String base, final Storage storage) {
        this.base = DownloadIndexSlice.url(base);
        this.storage = storage;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final String uri = line.uri().getPath();
        final Matcher matcher = DownloadIndexSlice.PTRN.matcher(uri);
        if (matcher.matches()) {
            final Key path = new KeyFromPath(uri);
            return new AsyncResponse(
                this.storage.exists(path).thenCompose(
                    exists -> {
                        final CompletionStage<Response> rsp;
                        if (exists) {
                            rsp = this.storage.value(path)
                                .thenCompose(
                                    content -> new UpdateIndexUrls(content, this.base).value()
                                ).thenApply(
                                    content -> BaseResponse.ok().body(content)
                                );
                        } else {
                            rsp = CompletableFuture.completedFuture(BaseResponse.notFound());
                        }
                        return rsp;
                    }
                )
            );
        }
        return BaseResponse.badRequest();
    }

    /**
     * Converts string with url to URL.
     * @param url String with url
     * @return URL from string with url.
     */
    private static URL url(final String url) {
        try {
            return URI.create(url.replaceAll("/$", "")).toURL();
        } catch (final MalformedURLException exc) {
            throw new ArtipieException(
                new IllegalStateException(
                    String.format("Failed to build URL from '%s'", url),
                    exc
                )
            );
        }
    }

    /**
     * Prepends all urls in the index file with the prefix to build
     * absolute URL: chart-0.4.1.tgz -&gt; http://host:port/path/chart-0.4.1.tgz.
     * @since 0.3
     */
    private static final class UpdateIndexUrls {
        /**
         * Original content.
         */
        private final Content original;

        /**
         * Base URL.
         */
        private final URL base;

        /**
         * Ctor.
         * @param original Original content
         * @param base Base URL
         */
        UpdateIndexUrls(final Content original, final URL base) {
            this.original = original;
            this.base = base;
        }

        /**
         * Return modified content with prepended URLs.
         * @return Modified content with prepended URLs
         */
        public CompletionStage<Content> value() {
            return this.original
                .asBytesFuture()
                .thenApply(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .thenApply(IndexYamlMapping::new)
                .thenApply(this::update)
                .thenApply(idx -> idx.toContent().orElseThrow());
        }

        /**
         * Updates urls for index file.
         * @param index Index yaml mapping
         * @return Index yaml mapping with updated urls.
         */
        private IndexYamlMapping update(final IndexYamlMapping index) {
            final Set<String> entrs = index.entries().keySet();
            entrs.forEach(
                chart -> index.byChart(chart).forEach(
                    entr -> {
                        final List<String> urls = new ChartYaml(entr).urls();
                        entr.put(
                            "urls",
                            urls.stream()
                                .map(this::baseUrlWithUri)
                                .collect(Collectors.toList())
                        );
                    }
                )
            );
            return index;
        }

        /**
         * Combine base url with uri.
         * @param uri Uri
         * @return Url that was obtained after combining.
         */
        private String baseUrlWithUri(final String uri) {
            final String unsafe = String.format("%s/%s", this.base, uri);
            try {
                return new URI(unsafe).toString();
            } catch (final URISyntaxException exc) {
                throw new IllegalStateException(
                    String.format("Failed to create URI from `%s`", unsafe),
                    exc
                );
            }
        }
    }
}
