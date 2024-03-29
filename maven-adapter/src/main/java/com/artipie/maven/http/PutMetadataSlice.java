/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.maven.metadata.DeployMetadata;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This slice accepts PUT requests with package (not snapshot) maven-metadata.xml,
 * reads `latest` version from the file and saves it to the temp location adding version and `meta`
 * before the filename:
 * `.upload/${package_name}/${version}/meta/maven-metadata.xml`.
 */
public final class PutMetadataSlice implements Slice {

    /**
     * Metadata sub-key.
     */
    public static final String SUB_META = "meta";

    /**
     * Metadata pattern.
     */
    static final Pattern PTN_META = Pattern.compile("^/(?<pkg>.+)/maven-metadata.xml$");

    /**
     * Maven metadata file name.
     */
    private static final String MAVEN_METADATA = "maven-metadata.xml";

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public PutMetadataSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final Matcher matcher = PutMetadataSlice.PTN_META.matcher(line.uri().getPath());
        if (matcher.matches()) {
            final Key pkg = new KeyFromPath(matcher.group("pkg"));
            return new Content.From(body).asStringFuture().thenCombine(
                this.asto.list(new Key.From(UploadSlice.TEMP, pkg)),
                (xml, list) -> {
                    final Optional<String> snapshot = new DeployMetadata(xml).snapshots()
                        .stream().filter(
                            item -> list.stream().anyMatch(key -> key.string().contains(item))
                        ).findFirst();
                    final Key key;
                    key = snapshot.map(s -> new Key.From(
                        UploadSlice.TEMP, pkg.string(), s,
                        PutMetadataSlice.SUB_META, PutMetadataSlice.MAVEN_METADATA
                    )).orElseGet(() -> new Key.From(
                        UploadSlice.TEMP, pkg.string(),
                        new DeployMetadata(xml).release(), PutMetadataSlice.SUB_META,
                        PutMetadataSlice.MAVEN_METADATA
                    ));
                    return this.asto.save(
                        key,
                        new Content.From(xml.getBytes(StandardCharsets.US_ASCII))
                    );
                }
            ).thenApply(nothing -> ResponseBuilder.created().build());
        }
        return CompletableFuture.completedFuture(ResponseBuilder.badRequest().build());
    }
}
