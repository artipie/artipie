/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PhpComposer}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PhpComposerTest {

    /**
     * Request line to get all packages.
     */
    private static final String GET_PACKAGES = new RequestLine(
        RqMethod.GET, "/packages.json"
    ).toString();

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
        this.php = new PhpComposer(new AstoRepository(this.storage));
    }

    @Test
    void shouldGetPackageContent() throws Exception {
        final byte[] data = "data".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("vendor", "package.json"),
            data
        );
        final Response response = this.php.response(
            new RequestLine(RqMethod.GET, "/p/vendor/package.json").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Package metadata should be returned in response",
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                )
            )
        );
    }

    @Test
    void shouldFailGetPackageMetadataWhenNotExists() {
        final Response response = this.php.response(
            new RequestLine(RqMethod.GET, "/p/vendor/unknown-package.json").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Not existing metadata should not be found",
            response,
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldGetAllPackages() throws Exception {
        final byte[] data = "all packages".getBytes();
        new BlockingStorage(this.storage).save(new AllPackages(), data);
        final Response response = this.php.response(
            PhpComposerTest.GET_PACKAGES,
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                )
            )
        );
    }

    @Test
    void shouldFailGetAllPackagesWhenNotExists() {
        final Response response = this.php.response(
            PhpComposerTest.GET_PACKAGES,
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldPutRoot() {
        final Response response = this.php.response(
            new RequestLine(RqMethod.PUT, "/").toString(),
            Collections.emptyList(),
            new Content.From(
                new TestResource("minimal-package.json").asBytes()
            )
        );
        MatcherAssert.assertThat(
            "Package should be created by put",
            response,
            new RsHasStatus(RsStatus.CREATED)
        );
    }
}
