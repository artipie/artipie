/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.common.RsError;
import com.artipie.pypi.NormalizedProjectName;
import com.artipie.pypi.meta.Metadata;
import com.artipie.pypi.meta.PackageInfo;
import com.jcabi.xml.XMLDocument;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Search slice.
 */
public final class SearchSlice implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public SearchSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new NameFromXml(body).get().thenCompose(
                name -> {
                    final Key.From key = new Key.From(
                        new NormalizedProjectName.Simple(name).value()
                    );
                    return this.storage.list(key).thenCompose(
                        list -> {
                            CompletableFuture<Content> res = new CompletableFuture<>();
                            if (list.isEmpty()) {
                                res.complete(new Content.From(SearchSlice.empty()));
                            } else {
                                final Key latest = list.stream().map(Key::string)
                                    .max(Comparator.naturalOrder())
                                    .map(Key.From::new)
                                    .orElseThrow(IllegalStateException::new);
                                res = this.storage.value(latest).thenCompose(
                                    val -> new ContentAsStream<PackageInfo>(val).process(
                                        input ->
                                            new Metadata.FromArchive(input, latest.string()).read()
                                    )
                                ).thenApply(info -> new Content.From(SearchSlice.found(info)));
                            }
                            return res;
                        }
                    );
                }
            ).handle(
                (content, throwable) -> {
                    final Response res;
                    if (throwable == null) {
                        res = new RsFull(
                            RsStatus.OK, new Headers.From("content-type", "text/xml"), content
                        );
                    } else {
                        res = new RsError(
                            new ArtipieHttpException(RsStatus.INTERNAL_ERROR, throwable)
                        );
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Response body when no packages found by given name.
     * @return Xml string
     */
    static byte[] empty() {
        return String.join(
            "\n", "<methodResponse>",
            "<params>",
            "<param>",
            "<value><array><data>",
            "</data></array></value>",
            "</param>",
            "</params>",
            "</methodResponse>"
        ).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Response body xml for search result.
     * @param info Package info
     * @return Xml string
     */
    static byte[] found(final PackageInfo info) {
        return String.join(
            "\n",
            "<?xml version='1.0'?>",
            "<methodResponse>",
            "<params>",
            "<param>",
            "<value><array><data>",
            "<value><struct>",
            "<member>",
            "<name>name</name>",
            String.format("<value><string>%s</string></value>", info.name()),
            "</member>",
            "<member>",
            "<name>summary</name>",
            String.format("<value><string>%s</string></value>", info.summary()),
            "</member>",
            "<member>",
            "<name>version</name>",
            String.format("<value><string>%s</string></value>", info.version()),
            "</member>",
            "<member>",
            "<name>_pypi_ordering</name>",
            "<value><boolean>0</boolean></value>",
            "</member>",
            "</struct></value>",
            "</data></array></value>",
            "</param>",
            "</params>",
            "</methodResponse>"
        ).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Python project name from request body xml.
     * @since 0.7
     */
    static final class NameFromXml {

        /**
         * Xml body.
         */
        private final Publisher<ByteBuffer> body;

        /**
         * Ctor.
         * @param body Body
         */
        NameFromXml(final Publisher<ByteBuffer> body) {
            this.body = body;
        }

        /**
         * Obtain project name to from xml.
         * @return Name of the project
         */
        CompletionStage<String> get() {
            final String query = "//member/value/array/data/value/string/text()";
            return new Content.From(this.body).asStringFuture().thenApply(
                xml -> new XMLDocument(xml)
                    .nodes("/*[local-name()='methodCall']/*[local-name()='params']/*[local-name()='param']/*[local-name()='value']/*[local-name()='struct']/*[local-name()='member']")
            ).thenApply(
                nodes -> nodes.stream()
                    .filter(
                        node -> "name".equals(node.xpath("//member/name/text()").get(0))
                            && !node.xpath(query).isEmpty()
                    )
                    .findFirst()
                    .map(node -> node.xpath(query))
                    .map(node -> node.get(0))
                    .orElseThrow(
                        () -> new IllegalArgumentException("Invalid xml, project name not found")
                    )
            );
        }
    }
}
