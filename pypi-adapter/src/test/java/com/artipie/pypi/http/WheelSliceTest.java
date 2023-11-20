/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link WheelSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class WheelSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Events queue.
     */
    private Queue<ArtifactEvent> queue;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.queue = new LinkedList<>();
    }

    @Test
    void savesContentAndReturnsOk() throws IOException {
        final String boundary = "simple boundary";
        final String filename = "artipie-sample-0.2.tar";
        final byte[] body = new TestResource("pypi_repo/artipie-sample-0.2.tar").asBytes();
        MatcherAssert.assertThat(
            "Returns CREATED status",
            new WheelSlice(this.asto, Optional.of(this.queue), "test"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, "/"),
                new Headers.From(
                    new ContentType(String.format("multipart/form-data; boundary=\"%s\"", boundary))
                ),
                new Content.From(this.multipartBody(body, boundary, filename))
            )
        );
        MatcherAssert.assertThat(
            "Saves content to storage",
            new PublisherAs(
                this.asto.value(new Key.From("artipie-sample", filename)).join()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat(
            "Added event to queue", this.queue.size() == 1
        );
    }

    @Test
    void savesContentByNormalizedNameAndReturnsOk() throws IOException {
        final String boundary = "my boundary";
        final String filename = "ABtests-0.0.2.1-py2.py3-none-any.whl";
        final String path = "super";
        final byte[] body = new TestResource("pypi_repo/ABtests-0.0.2.1-py2.py3-none-any.whl")
            .asBytes();
        MatcherAssert.assertThat(
            "Returns CREATED status",
            new WheelSlice(this.asto, Optional.of(this.queue), "TEST"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine("POST", String.format("/%s", path)),
                new Headers.From(
                    new ContentType(String.format("multipart/form-data; boundary=\"%s\"", boundary))
                ),
                new Content.From(this.multipartBody(body, boundary, filename))
            )
        );
        MatcherAssert.assertThat(
            "Saves content to storage",
            new PublisherAs(
                this.asto.value(new Key.From(path, "abtests", filename)).join()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat(
            "Added event to queue", this.queue.size() == 1
        );
    }

    @Test
    void returnsBadRequestIfFileNameIsInvalid() throws IOException {
        final String boundary = RandomStringUtils.random(10);
        final String filename = "artipie-sample-2020.tar.bz2";
        final byte[] body = new TestResource("pypi_repo/artipie-sample-2.1.tar.bz2").asBytes();
        MatcherAssert.assertThat(
            "Returns BAD_REQUEST status",
            new WheelSlice(this.asto, Optional.of(this.queue), "test"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.POST, "/"),
                new Headers.From(
                    new ContentType(String.format("multipart/form-data; boundary=\"%s\"", boundary))
                ),
                new Content.From(this.multipartBody(body, boundary, filename))
                )
        );
        MatcherAssert.assertThat(
            "Storage is empty",
            this.asto.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
        );
        MatcherAssert.assertThat(
            "Event to queue is empty", this.queue.isEmpty()
        );
    }

    @Test
    void returnsBadRequestIfFileInvalid() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String boundary = RandomStringUtils.random(10);
        final String filename = "myproject.whl";
        final byte[] body = "some code".getBytes();
        MatcherAssert.assertThat(
            new WheelSlice(storage, Optional.of(this.queue), "test"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.POST, "/"),
                new Headers.From(
                    new ContentType(String.format("multipart/form-data; boundary=\"%s\"", boundary))
            ),
                new Content.From(this.multipartBody(body, boundary, filename))
            )
        );
        MatcherAssert.assertThat(
            "Event to queue is empty", this.queue.isEmpty()
        );
    }

    private byte[] multipartBody(final byte[] input, final String boundary, final String filename)
        throws IOException {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(
            String.join(
                "\r\n",
                "Ignored preamble",
                String.format("--%s", boundary),
                "Content-Disposition: form-data; name=\"data\"",
                "",
                "",
                "some data",
                String.format("--%s", boundary),
                String.format(
                    "Content-Disposition: form-data; name=\"content\"; filename=\"%s\"",
                    filename
                ),
                "",
                ""
            ).getBytes(StandardCharsets.US_ASCII)
        );
        body.write(input);
        body.write(String.format("\r\n--%s--", boundary).getBytes(StandardCharsets.US_ASCII));
        return body.toByteArray();
    }

}
