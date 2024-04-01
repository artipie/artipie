/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.ResponseBuilder;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * Http NPM Remote client test.
 */
public final class HttpNpmRemoteTest {

    /**
     * Last modified date for both package and asset.
     */
    private static final String LAST_MODIFIED = "Tue, 24 Mar 2020 12:15:16 GMT";

    /**
     * Asset Content-Type.
     */
    private static final String DEF_CONTENT_TYPE = "application/octet-stream";

    /**
     * Assert content.
     */
    private static final String DEF_CONTENT = "foobar";

    /**
     * NPM Remote client instance.
     */
    private HttpNpmRemote remote;

    @Test
    void loadsPackage() throws IOException, JSONException, InterruptedException {
        final String name = "asdas";
        final OffsetDateTime started = OffsetDateTime.now();
        Thread.sleep(100);
        final NpmPackage pkg = this.remote.loadPackage(name).blockingGet();
        MatcherAssert.assertThat("Package is null", pkg != null);
        MatcherAssert.assertThat(
            "Package name is correct",
            pkg.name(),
            new IsEqual<>(name)
        );
        JSONAssert.assertEquals(
            IOUtils.resourceToString("/json/cached.json", StandardCharsets.UTF_8),
            pkg.content(),
            true
        );
        MatcherAssert.assertThat(
            "Metadata last modified date is correct",
            pkg.meta().lastModified(),
            new IsEqual<>(HttpNpmRemoteTest.LAST_MODIFIED)
        );
        final OffsetDateTime checked = OffsetDateTime.now();
        MatcherAssert.assertThat(
            String.format(
                "Unexpected last refreshed date: %s (started: %s, checked: %s)",
                    pkg.meta().lastRefreshed(),
                started,
                checked
            ),
            pkg.meta().lastRefreshed().isAfter(started)
                && !pkg.meta().lastRefreshed().isAfter(checked)
        );
    }

    @Test
    void loadsAsset() throws IOException {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        final Path tmp = Files.createTempFile("npm-asset-", "tmp");
        try {
            final NpmAsset asset = this.remote.loadAsset(path, tmp).blockingGet();
            MatcherAssert.assertThat("Asset is null", asset != null);
            MatcherAssert.assertThat(
                "Path to asset is correct",
                asset.path(),
                new IsEqual<>(path)
            );
            MatcherAssert.assertThat(
                "Content of asset is correct",
                new Content.From(asset.dataPublisher()).asString(),
                new IsEqual<>(HttpNpmRemoteTest.DEF_CONTENT)
            );
            MatcherAssert.assertThat(
                "Modified date is correct",
                asset.meta().lastModified(),
                new IsEqual<>(HttpNpmRemoteTest.LAST_MODIFIED)
            );
            MatcherAssert.assertThat(
                "Content-type of asset is correct",
                asset.meta().contentType(),
                new IsEqual<>(HttpNpmRemoteTest.DEF_CONTENT_TYPE)
            );
        } finally {
            Files.delete(tmp);
        }
    }

    @Test
    void doesNotFindPackage() {
        final Boolean empty = this.remote.loadPackage("not-found").isEmpty().blockingGet();
        MatcherAssert.assertThat("Unexpected package found", empty);
    }

    @Test
    void doesNotFindAsset() throws IOException {
        final Path tmp = Files.createTempFile("npm-asset-", "tmp");
        try {
            final Boolean empty = this.remote.loadAsset("not-found", tmp)
                .isEmpty().blockingGet();
            MatcherAssert.assertThat("Unexpected asset found", empty);
        } finally {
            Files.delete(tmp);
        }
    }

    @BeforeEach
    void setUp() {
        this.remote = new HttpNpmRemote(this.prepareClientSlice());
    }

    private Slice prepareClientSlice() {
        return (line, headers, body) -> {
            final String path = line.uri().getPath();
            if (path.equalsIgnoreCase("/asdas")) {
                return ResponseBuilder.ok()
                    .header("Last-Modified", HttpNpmRemoteTest.LAST_MODIFIED)
                    .body(new TestResource("json/original.json").asBytes())
                    .completedFuture();
            }
            if (path.equalsIgnoreCase("/asdas/-/asdas-1.0.0.tgz")) {
                return ResponseBuilder.ok()
                    .header(new Header("Last-Modified", HttpNpmRemoteTest.LAST_MODIFIED))
                    .header(ContentType.mime(HttpNpmRemoteTest.DEF_CONTENT_TYPE))
                    .body(HttpNpmRemoteTest.DEF_CONTENT.getBytes(StandardCharsets.UTF_8))
                    .completedFuture();
            }
            return ResponseBuilder.notFound().completedFuture();
        };
    }
}
