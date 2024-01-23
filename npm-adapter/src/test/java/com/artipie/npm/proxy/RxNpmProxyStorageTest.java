/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NPM Proxy storage test.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RxNpmProxyStorageTest {
    /**
     * Last modified date for both package and asset.
     */
    private static final String MODIFIED = "Tue, 24 Mar 2020 12:15:16 GMT";

    /**
     * Last refreshed date for package (datetime).
     */
    private static final OffsetDateTime REFRESHED = OffsetDateTime.of(
        LocalDateTime.of(2020, Month.APRIL, 24, 12, 15, 16, 123_456_789),
        ZoneOffset.UTC
    );

    /**
     * Last refreshed date for package (string).
     */
    private static final String REFRESHED_STR = "2020-04-24T12:15:16.123456789Z";

    /**
     * Asset Content-Type.
     */
    private static final String CONTENT_TYPE = "application/octet-stream";

    /**
     * Assert content.
     */
    private static final String DEF_CONTENT = "foobar";

    /**
     * NPM Proxy Storage.
     */
    private NpmProxyStorage storage;

    /**
     * Underlying storage.
     */
    private Storage delegate;

    @Test
    public void savesPackage() throws IOException {
        this.doSavePackage("asdas", RxNpmProxyStorageTest.REFRESHED);
        MatcherAssert.assertThat(
            this.publisherAsStr("asdas/meta.json"),
            new IsEqual<>(RxNpmProxyStorageTest.readContent())
        );
        final String metadata = this.publisherAsStr("asdas/meta.meta");
        final JsonObject json = new JsonObject(metadata);
        MatcherAssert.assertThat(
            json.getString("last-modified"),
            new IsEqual<>(RxNpmProxyStorageTest.MODIFIED)
        );
        MatcherAssert.assertThat(
            new JsonObject(metadata).getString("last-refreshed"),
            new IsEqual<>(RxNpmProxyStorageTest.REFRESHED_STR)
        );
    }

    @Test
    public void savesAsset() {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        this.doSaveAsset(path);
        MatcherAssert.assertThat(
            "Content of asset is correct",
            this.publisherAsStr(path),
            new IsEqual<>(RxNpmProxyStorageTest.DEF_CONTENT)
        );
        final String metadata = this.publisherAsStr("asdas/-/asdas-1.0.0.tgz.meta");
        final JsonObject json = new JsonObject(metadata);
        MatcherAssert.assertThat(
            "Last-modified is correct",
            json.getString("last-modified"),
            new IsEqual<>(RxNpmProxyStorageTest.MODIFIED)
        );
        MatcherAssert.assertThat(
            "Content-type of asset is correct",
            json.getString("content-type"),
            new IsEqual<>(RxNpmProxyStorageTest.CONTENT_TYPE)
        );
    }

    @Test
    public void loadsPackage() throws IOException {
        final String name = "asdas";
        this.doSavePackage(name, RxNpmProxyStorageTest.REFRESHED);
        final NpmPackage pkg = this.storage.getPackage(name).blockingGet();
        MatcherAssert.assertThat(
            "Package name is correct",
            pkg.name(),
            new IsEqual<>(name)
        );
        MatcherAssert.assertThat(
            "Content of package is correct",
            pkg.content(),
            new IsEqual<>(RxNpmProxyStorageTest.readContent())
        );
        MatcherAssert.assertThat(
            "Modified date is correct",
            pkg.meta().lastModified(),
            new IsEqual<>(RxNpmProxyStorageTest.MODIFIED)
        );
        MatcherAssert.assertThat(
            "Refreshed date is correct",
            pkg.meta().lastRefreshed(),
            new IsEqual<>(RxNpmProxyStorageTest.REFRESHED)
        );
    }

    @Test
    public void loadsAsset() {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        this.doSaveAsset(path);
        final NpmAsset asset = this.storage.getAsset(path).blockingGet();
        MatcherAssert.assertThat(
            "Path to asset is correct",
            asset.path(),
            new IsEqual<>(path)
        );
        MatcherAssert.assertThat(
            "Content of asset is correct",
            new PublisherAs(asset.dataPublisher())
                .asciiString()
                .toCompletableFuture().join(),
            new IsEqual<>(RxNpmProxyStorageTest.DEF_CONTENT)
        );
        MatcherAssert.assertThat(
            "Modified date is correct",
            asset.meta().lastModified(),
            new IsEqual<>(RxNpmProxyStorageTest.MODIFIED)
        );
        MatcherAssert.assertThat(
            "Content-type of asset is correct",
            asset.meta().contentType(),
            new IsEqual<>(RxNpmProxyStorageTest.CONTENT_TYPE)
        );
    }

    @Test
    public void failsToLoadPackage() {
        MatcherAssert.assertThat(
            "Unexpected package found",
            this.storage.getPackage("not-found").isEmpty().blockingGet()
        );
    }

    @Test
    public void failsToLoadAsset() {
        MatcherAssert.assertThat(
            "Unexpected package asset",
            this.storage.getAsset("not-found").isEmpty().blockingGet()
        );
    }

    @BeforeEach
    void setUp() {
        this.delegate = new InMemoryStorage();
        this.storage = new RxNpmProxyStorage(new RxStorageWrapper(this.delegate));
    }

    private String publisherAsStr(final String path) {
        return new PublisherAs(
            this.delegate.value(new Key.From(path)).join()
        ).asciiString()
        .toCompletableFuture().join();
    }

    private void doSavePackage(final String name, final OffsetDateTime refreshed)
        throws IOException {
        this.storage.save(
            new NpmPackage(
                name,
                RxNpmProxyStorageTest.readContent(),
                RxNpmProxyStorageTest.MODIFIED,
                refreshed
            )
        ).blockingAwait();
    }

    private void doSaveAsset(final String path) {
        this.storage.save(
            new NpmAsset(
                path,
                new Content.From(RxNpmProxyStorageTest.DEF_CONTENT.getBytes()),
                RxNpmProxyStorageTest.MODIFIED,
                RxNpmProxyStorageTest.CONTENT_TYPE
           )
        ).blockingAwait();
    }

    private static String readContent() throws IOException {
        return IOUtils.resourceToString(
            "/json/cached.json",
            StandardCharsets.UTF_8
        );
    }
}
