/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.maven.metadata.DeployMetadata;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * This slice accepts PUT requests with package (not snapshot) maven-metadata.xml,
 * reads `latest` version from the file and saves it to the temp location adding version and `meta`
 * before the filename:
 * `.upload/${package_name}/${version}/meta/maven-metadata.xml`.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response res;
        final Matcher matcher = PutMetadataSlice.PTN_META.matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        if (matcher.matches()) {
            final Key pkg = new KeyFromPath(matcher.group("pkg"));
            res = new AsyncResponse(
                new PublisherAs(body).asciiString().thenCombine(
                    this.asto.list(new Key.From(UploadSlice.TEMP, pkg)),
                    (xml, list) -> {
                        final Optional<String> snapshot = new DeployMetadata(xml).snapshots()
                            .stream().filter(
                                item -> list.stream().anyMatch(key -> key.string().contains(item))
                            ).findFirst();
                        final Key key;
                        if (snapshot.isPresent()) {
                            key = new Key.From(
                                UploadSlice.TEMP, pkg.string(), snapshot.get(),
                                PutMetadataSlice.SUB_META, PutMetadataSlice.MAVEN_METADATA
                            );
                        } else {
                            key = new Key.From(
                                UploadSlice.TEMP, pkg.string(),
                                new DeployMetadata(xml).release(), PutMetadataSlice.SUB_META,
                                PutMetadataSlice.MAVEN_METADATA
                            );
                        }
                        return this.asto.save(
                            key,
                            new Content.From(xml.getBytes(StandardCharsets.US_ASCII))
                        );
                    }
                ).thenApply(nothing -> new RsWithStatus(RsStatus.CREATED))
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }
}
