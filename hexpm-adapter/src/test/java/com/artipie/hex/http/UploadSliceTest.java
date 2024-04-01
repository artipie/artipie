/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.hex.ResourceUtil;
import com.artipie.hex.proto.generated.PackageOuterClass;
import com.artipie.hex.proto.generated.SignedOuterClass;
import com.artipie.hex.utils.Gzip;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link UploadSlice}.
 */
class UploadSliceTest {
    /**
     * Tar archive as byte array.
     */
    private static byte[] tar;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * UploadSlice.
     */
    private Slice slice;

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    @BeforeAll
    static void beforeAll() throws IOException {
        UploadSliceTest.tar = Files.readAllBytes(
            new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath()
        );
    }

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.events = new LinkedList<>();
        this.slice = new UploadSlice(this.storage, Optional.of(this.events), "my-hexpm-test");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void publish(final boolean replace) throws Exception {
        MatcherAssert.assertThat(
            "Wrong response status, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, String.format("/publish?replace=%s", replace)),
                Headers.from(new ContentLength(UploadSliceTest.tar.length)),
                new Content.From(UploadSliceTest.tar)
            )
        );
        MatcherAssert.assertThat(
            "Package was not saved in storage",
            this.storage.value(new Key.From("packages/decimal")).join(),
            new ContentIs(Files.readAllBytes(new ResourceUtil("packages/decimal").asPath()))
        );
        MatcherAssert.assertThat(
            "Tarball was not saved in storage",
            this.storage.value(new Key.From("tarballs", "decimal-2.0.0.tar")).join(),
            new ContentIs(UploadSliceTest.tar)
        );
        MatcherAssert.assertThat(
            "Package is not filled",
            this.checkPackage("decimal", "2.0.0", DigestUtils.sha256Hex(UploadSliceTest.tar)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
        final ArtifactEvent event = this.events.poll();
        MatcherAssert.assertThat(
            "Package name should be decimal", event.artifactName(), new IsEqual<>("decimal")
        );
        MatcherAssert.assertThat(
            "Package version should be 2.0.0", event.artifactVersion(), new IsEqual<>("2.0.0")
        );
    }

    @Test
    void publishExistedPackageReplaceFalse() {
        MatcherAssert.assertThat(
            "Wrong response status for the first upload, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, "/publish?replace=false"),
                Headers.from(new ContentLength(UploadSliceTest.tar.length)),
                new Content.From(UploadSliceTest.tar)
            )
        );
        MatcherAssert.assertThat(
            "Wrong response status for a package that already exists, INTERNAL_ERROR is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.INTERNAL_ERROR),
                new RequestLine(RqMethod.POST, "/publish?replace=false"),
                Headers.from(new ContentLength(UploadSliceTest.tar.length)),
                new Content.From(UploadSliceTest.tar)
            )
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @Test
    void publishExistedPackageReplaceTrue() throws Exception {
        MatcherAssert.assertThat(
            "Wrong response status for the first upload, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, "/publish?replace=false"),
                Headers.from(new ContentLength(UploadSliceTest.tar.length)),
                new Content.From(UploadSliceTest.tar)
            )
        );
        final byte[] replacement = Files.readAllBytes(
            new ResourceUtil("tarballs/extended_decimal-2.0.0.tar").asPath()
        );
        MatcherAssert.assertThat(
            "Wrong response status for upload with tar replace, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, "/publish?replace=true"),
                Headers.from(new ContentLength(replacement.length)),
                new Content.From(replacement)
            )
        );
        MatcherAssert.assertThat(
            "Version not replaced",
            this.checkPackage("decimal", "2.0.0", DigestUtils.sha256Hex(replacement)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 2);
    }

    @Test
    void returnsBadRequestOnIncorrectRequest() {
        MatcherAssert.assertThat(
            "Wrong response status, BAD_REQUEST is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.POST, "/publish")
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    private boolean checkPackage(
        final String name,
        final String version,
        final String outerchecksum
    ) throws Exception {
        boolean result = false;
        final byte[] gzippedbytes =
            new Concatenation(
                new OneTimePublisher<>(
                    this.storage.value(new Key.From(DownloadSlice.PACKAGES, name)).join()
                )
            ).single()
                .to(SingleInterop.get())
                .thenApply(Remaining::new)
                .thenApply(Remaining::bytes)
                .toCompletableFuture()
                .join();
        final byte[] bytes = new Gzip(gzippedbytes).decompress();
        final SignedOuterClass.Signed signed = SignedOuterClass.Signed.parseFrom(bytes);
        final PackageOuterClass.Package pkg =
            PackageOuterClass.Package.parseFrom(signed.getPayload());
        final List<PackageOuterClass.Release> releases = pkg.getReleasesList();
        for (final PackageOuterClass.Release release : releases) {
            if (release.getVersion().equals(version)
                && outerchecksum.equals(
                    new String(Hex.encodeHex(release.getOuterChecksum().toByteArray()))
                )
            ) {
                result = true;
            }
        }
        return result;
    }

}
