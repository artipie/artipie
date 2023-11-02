/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.maven.MetadataXml;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PutMetadataSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PutMetadataSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Test slice.
     */
    private Slice pms;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.pms = new PutMetadataSlice(this.asto);
    }

    @Test
    void returnsCreatedAndSavesMetadata() {
        final byte[] xml = new MetadataXml("com.example", "any").get(
            new MetadataXml.VersionTags("0.1", "0.2", new ListOf<String>("0.1", "0.2"))
        ).getBytes(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            "Incorrect response status, CREATED is expected",
            this.pms,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.PUT, "/com/example/any/maven-metadata.xml"),
                new Headers.From(new ContentLength(xml.length)),
                new Content.OneTime(new Content.From(xml))
            )
        );
        MatcherAssert.assertThat(
            "Metadata file was not saved to storage",
            this.asto.value(
                new Key.From(".upload/com/example/any/0.2/meta/maven-metadata.xml")
            ).join(),
            new ContentIs(xml)
        );
    }

    @Test
    void returnsCreatedAndSavesSnapshotMetadata() {
        final byte[] xml = new MetadataXml("com.example", "abc").get(
            new MetadataXml.VersionTags("0.1-SNAPSHOT", "0.2-SNAPSHOT")
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.2-SNAPSHOT/abc.jar"), Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            "Incorrect response status, CREATED is expected",
            this.pms,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.PUT, "/com/example/abc/maven-metadata.xml"),
                new Headers.From(new ContentLength(xml.length)),
                new Content.OneTime(new Content.From(xml))
            )
        );
        MatcherAssert.assertThat(
            "Metadata file was not saved to storage",
            this.asto.value(
                new Key.From(".upload/com/example/abc/0.2-SNAPSHOT/meta/maven-metadata.xml")
            ).join(),
            new ContentIs(xml)
        );
    }

    @Test
    void returnsCreatedAndSavesSnapshotMetadataWhenReleaseIsPresent() {
        final byte[] xml = new MetadataXml("com.example", "any").get(
            new MetadataXml.VersionTags(
                Optional.empty(), Optional.of("0.2"),
                new ListOf<String>("0.1", "0.2", "0.3-SNAPSHOT")
            )
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/any/0.3-SNAPSHOT/any.jar"), Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            "Incorrect response status, CREATED is expected",
            this.pms,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.PUT, "/com/example/any/maven-metadata.xml"),
                new Headers.From(new ContentLength(xml.length)),
                new Content.OneTime(new Content.From(xml))
            )
        );
        MatcherAssert.assertThat(
            "Metadata file was not saved to storage",
            this.asto.value(
                new Key.From(".upload/com/example/any/0.3-SNAPSHOT/meta/maven-metadata.xml")
            ).join(),
            new ContentIs(xml)
        );
    }

    @Test
    void returnsBadRequestWhenRqLineIsIncorrect() {
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            this.pms,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/abc/123")
            )
        );
    }

}
