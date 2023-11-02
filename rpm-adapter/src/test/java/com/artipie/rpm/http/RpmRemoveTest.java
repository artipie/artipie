/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.rpm.RepoConfig;
import com.artipie.scheduling.ArtifactEvent;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.zip.GZIPInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for {@link RpmRemove}.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RpmRemoveTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsAcceptedAndDoesNotRemoveAndDoNotCheckChecksum() {
        final String pckg = "my_package.rpm";
        this.asto.save(new Key.From(pckg), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Response status is not `ACCEPTED`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), Optional.empty()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.ACCEPTED),
                new RequestLine(RqMethod.DELETE, "/my_package.rpm?skip_update=true&force=true"),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Storage should have package",
            this.asto.exists(new Key.From(pckg)).join()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/test_package.rpm?skip_update=true",
        "/test_package.rpm?skip_update=true&force=false"
    })
    void returnsAcceptedAndDoesNotRemove(final String line) {
        final String pckg = "test_package.rpm";
        final byte[] bytes = "pkg".getBytes(StandardCharsets.US_ASCII);
        this.asto.save(new Key.From(pckg), new Content.From(bytes)).join();
        final Optional<Queue<ArtifactEvent>> events = Optional.of(new LinkedList<>());
        MatcherAssert.assertThat(
            "Response status is not `ACCEPTED`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), events),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.ACCEPTED),
                new RequestLine(RqMethod.DELETE, line),
                new Headers.From("X-Checksum-sha-256", DigestUtils.sha256Hex(bytes)),
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Storage should have package",
            this.asto.exists(new Key.From(pckg)).join()
        );
        MatcherAssert.assertThat("Events queue is empty", events.get().size() == 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "force=true&skip_update=false",
        "force=true"
    })
    void returnsAcceptedAndRemoves(final String params) throws IOException {
        final String pckg = "abc-1.01-26.git20200127.fc32.ppc64le.rpm";
        new TestResource(pckg).saveTo(this.asto);
        new TestResource("RpmRemoveTest/primary.xml.gz")
            .saveTo(this.asto, new Key.From("repodata", "primary.xml.gz"));
        final Optional<Queue<ArtifactEvent>> events = Optional.of(new LinkedList<>());
        MatcherAssert.assertThat(
            "Response status is not `ACCEPTED`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), events),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.ACCEPTED),
                new RequestLine(RqMethod.DELETE, String.format("/%s?%s", pckg, params)),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Storage should not have package `abc`",
            !this.asto.exists(new Key.From(pckg)).join()
        );
        MatcherAssert.assertThat(
            "Repomd xml should be created",
            new String(
                new BlockingStorage(this.asto).value(new Key.From("repodata", "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='primary']"
            )
        );
        MatcherAssert.assertThat(
            "Primary xml should have `nginx` record",
            new String(
                this.readAndUnpack(new Key.From("repodata", "primary.xml.gz")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='1']",
                //@checkstyle LineLengthCheck (1 line)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']"
            )
        );
        MatcherAssert.assertThat("Events queue has one item", events.get().size() == 1);
    }

    @Test
    void returnsBadRequestIfFileDoesNotExist() {
        MatcherAssert.assertThat(
            "Response status is not `BAD_REQUEST`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), Optional.empty()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/any.rpm"),
                new Headers.From("X-Checksum-sha-256", "abc123"),
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Storage should be empty",
            this.asto.list(Key.ROOT).join(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void returnsBadRequestIfHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            "Response status is not `BAD_REQUEST`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), Optional.empty()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/any_package.rpm")
            )
        );
        MatcherAssert.assertThat(
            "Storage should be empty",
            this.asto.list(Key.ROOT).join(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void returnsBadRequestIfChecksumIsIncorrect() {
        final String pckg = "my_package.rpm";
        this.asto.save(new Key.From(pckg), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Response status is not `BAD_REQUEST`",
            new RpmRemove(this.asto, new RepoConfig.Simple(), Optional.empty()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/my_package.rpm"),
                new Headers.From("x-checksum-md5", "abc123"),
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Storage should have 1 value",
            this.asto.list(Key.ROOT).join(),
            Matchers.iterableWithSize(1)
        );
    }

    private byte[] readAndUnpack(final Key key) throws IOException {
        return IOUtils.toByteArray(
            new GZIPInputStream(
                new ByteArrayInputStream(new BlockingStorage(this.asto).value(key))
            )
        );
    }

}
