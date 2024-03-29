/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.content;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.http.NuGet;
import com.artipie.nuget.http.TestAuthentication;
import com.artipie.security.policy.PolicyByUsername;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

/**
 * Tests for {@link NuGet}.
 * Package Content resource.
 */
class NuGetPackageContentTest {

    private Storage storage;

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        this.nuget = new NuGet(
            URI.create("http://localhost").toURL(),
            new AstoRepository(this.storage),
            new PolicyByUsername(TestAuthentication.USERNAME),
            new TestAuthentication(),
            "test",
            Optional.empty()
        );
    }

    @Test
    void shouldGetPackageContent() {
        final byte[] data = "data".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package", "1.0.0", "content.nupkg"),
            data
        );
        ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/content/package/1.0.0/content.nupkg"
            ), TestAuthentication.HEADERS, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertArrayEquals(data, response.body().asBytes());
    }

    @Test
    void shouldFailGetPackageContentWhenNotExists() {
        ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/content/package/1.0.0/logo.png"
            ), TestAuthentication.HEADERS, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.NOT_FOUND, response.status());
    }

    @Test
    void shouldFailPutPackageContent() {
        final ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.PUT,
                "/content/package/1.0.0/content.nupkg"
            ), TestAuthentication.HEADERS, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.METHOD_NOT_ALLOWED, response.status());
    }

    @Test
    void shouldGetPackageVersions() {
        final byte[] data = "example".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package2", "index.json"),
            data
        );
        final ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/content/package2/index.json"
            ), TestAuthentication.HEADERS, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertArrayEquals(data, response.body().asBytes());
    }

    @Test
    void shouldFailGetPackageVersionsWhenNotExists() {
        final ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/content/unknown-package/index.json"
            ), TestAuthentication.HEADERS, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.NOT_FOUND, response.status());
    }

    @Test
    void shouldUnauthorizedGetPackageContentByAnonymousUser() {
        final ResponseImpl response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/content/package/2.0.0/content.nupkg"
            ), Headers.EMPTY, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.UNAUTHORIZED, response.status());
    }
}
