/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AllPackages;
import com.artipie.composer.AstoRepository;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.policy.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link PhpComposer}.
 */
class PhpComposerTest {

    /**
     * Request line to get all packages.
     */
    private static final RequestLine GET_PACKAGES = new RequestLine(
        RqMethod.GET, "/packages.json"
    );

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Tested PhpComposer slice.
     */
    private PhpComposer php;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.php = new PhpComposer(
            new AstoRepository(this.storage),
            Policy.FREE, (username, password) -> Optional.empty(),
            "*", Optional.empty()
        );
    }

    @Test
    void shouldGetPackageContent() throws Exception {
        final byte[] data = "data".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("vendor", "package.json"),
            data
        );
        final ResponseImpl response = this.php.response(
            new RequestLine(RqMethod.GET, "/p/vendor/package.json"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertArrayEquals(data, response.body().asBytes());
    }

    @Test
    void shouldFailGetPackageMetadataWhenNotExists() {
        final ResponseImpl response = this.php.response(
            new RequestLine(RqMethod.GET, "/p/vendor/unknown-package.json"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.NOT_FOUND, response.status());
    }

    @Test
    void shouldGetAllPackages() throws Exception {
        final byte[] data = "all packages".getBytes();
        new BlockingStorage(this.storage).save(new AllPackages(), data);
        final ResponseImpl response = this.php.response(
            PhpComposerTest.GET_PACKAGES,
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertEquals("all packages", response.body().asString());
    }

    @Test
    void shouldFailGetAllPackagesWhenNotExists() {
        final ResponseImpl response = this.php.response(
            PhpComposerTest.GET_PACKAGES,
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.NOT_FOUND, response.status());
    }

    @Test
    void shouldPutRoot() {
        final ResponseImpl response = this.php.response(
            new RequestLine(RqMethod.PUT, "/"),
            Headers.EMPTY,
            new Content.From(
                new TestResource("minimal-package.json").asBytes()
            )
        ).join();
        Assertions.assertEquals(RsStatus.CREATED, response.status());
    }
}
