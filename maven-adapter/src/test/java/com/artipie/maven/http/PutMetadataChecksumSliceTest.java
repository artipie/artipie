/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.maven.Maven;
import com.artipie.maven.MetadataXml;
import com.artipie.maven.ValidUpload;
import com.artipie.scheduling.ArtifactEvent;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link PutMetadataChecksumSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PutMetadataChecksumSliceTest {

    /**
     * Maven test repository name.
     */
    private static final String TEST_NAME = "my-test-maven";

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.events = new LinkedList<>();
    }

    @ParameterizedTest
    @ValueSource(strings = {"md5", "sha1", "sha256", "sha512"})
    void foundsCorrespondingMetadataAndUpdatesRepo(final String alg) {
        final byte[] xml = new MetadataXml("com.example", "abc").get(
            new MetadataXml.VersionTags("0.1")
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.1/meta/maven-metadata.xml"),
            new Content.From(xml)
        ).join();
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.2/meta/maven-metadata.xml"),
            new Content.From("any".getBytes())
        ).join();
        final byte[] mdfive = new ContentDigest(
            new Content.From(xml), Digests.valueOf(alg.toUpperCase(Locale.US))
        ).hex().toCompletableFuture().join().getBytes(StandardCharsets.US_ASCII);
        final Maven.Fake mvn = new Maven.Fake(this.asto);
        MatcherAssert.assertThat(
            "Incorrect response status, CREATED is expected",
            new PutMetadataChecksumSlice(
                this.asto, new ValidUpload.Dummy(), mvn,
                PutMetadataChecksumSliceTest.TEST_NAME, Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(
                    RqMethod.PUT, String.format("/com/example/abc/maven-metadata.xml.%s", alg)
                ),
                Headers.EMPTY,
                new Content.From(mdfive)
            )
        );
        MatcherAssert.assertThat(
            "Incorrect content was saved to storage",
            this.asto.value(
                new Key.From(
                    String.format("com/example/abc/0.1/meta/maven-metadata.xml.%s", alg)
                )
            ).join(),
            new ContentIs(mdfive)
        );
        MatcherAssert.assertThat(
            "Repository update was not started",
            mvn.wasUpdated(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Upload event was added to queue", this.events.size() == 1);
    }

    @Test
    void returnsBadRequestWhenRepositoryIsNotValid() {
        final byte[] xml = new MetadataXml("com.example", "abc").get(
            new MetadataXml.VersionTags("0.1")
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.1/meta/maven-metadata.xml"),
            new Content.From(xml)
        ).join();
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.2/meta/maven-metadata.xml"),
            new Content.From("any".getBytes())
        ).join();
        final String alg = "md5";
        final byte[] mdfive = new ContentDigest(
            new Content.From(xml), Digests.valueOf(alg.toUpperCase(Locale.US))
        ).hex().toCompletableFuture().join().getBytes(StandardCharsets.US_ASCII);
        final Maven.Fake mvn = new Maven.Fake(this.asto);
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new PutMetadataChecksumSlice(
                this.asto, new ValidUpload.Dummy(false, true), mvn,
                PutMetadataChecksumSliceTest.TEST_NAME, Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(
                    RqMethod.PUT, String.format("/com/example/abc/maven-metadata.xml.%s", alg)
                ),
                Headers.EMPTY,
                new Content.From(mdfive)
            )
        );
        MatcherAssert.assertThat(
            "Repository update was started when it does not supposed to",
            mvn.wasUpdated(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Upload events queue is empty", this.events.size() == 0);
    }

    @Test
    void returnsCreatedWhenRepositoryIsNotReady() {
        final byte[] xml = new MetadataXml("com.example", "abc").get(
            new MetadataXml.VersionTags("0.1")
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.1/meta/maven-metadata.xml"),
            new Content.From(xml)
        ).join();
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/abc/0.2/meta/maven-metadata.xml"),
            new Content.From("any".getBytes())
        ).join();
        final String alg = "sha1";
        final byte[] mdfive = new ContentDigest(
            new Content.From(xml), Digests.valueOf(alg.toUpperCase(Locale.US))
        ).hex().toCompletableFuture().join().getBytes(StandardCharsets.US_ASCII);
        final Maven.Fake mvn = new Maven.Fake(this.asto);
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new PutMetadataChecksumSlice(
                this.asto, new ValidUpload.Dummy(false, false), mvn,
                PutMetadataChecksumSliceTest.TEST_NAME, Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(
                    RqMethod.PUT, String.format("/com/example/abc/maven-metadata.xml.%s", alg)
                ),
                Headers.EMPTY,
                new Content.From(mdfive)
            )
        );
        MatcherAssert.assertThat(
            "Incorrect content was saved to storage",
            this.asto.value(
                new Key.From(
                    String.format(".upload/com/example/abc/0.1/meta/maven-metadata.xml.%s", alg)
                )
            ).join(),
            new ContentIs(mdfive)
        );
        MatcherAssert.assertThat(
            "Repository update was started when it does not supposed to",
            mvn.wasUpdated(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Upload events queue is empty", this.events.isEmpty());
    }

    @Test
    void returnsBadRequestIfSuitableMetadataFileNotFound() {
        this.asto.save(
            new Key.From(UploadSlice.TEMP, "com/example/xyz/0.1/meta/maven-metadata.xml"),
            new Content.From("xml".getBytes())
        ).join();
        final Maven.Fake mvn = new Maven.Fake(this.asto);
        MatcherAssert.assertThat(
            "Incorrect response status, BAD REQUEST is expected",
            new PutMetadataChecksumSlice(
                this.asto, new ValidUpload.Dummy(), mvn,
                PutMetadataChecksumSliceTest.TEST_NAME, Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/com/example/xyz/maven-metadata.xml.sha1"),
                Headers.EMPTY,
                new Content.From("any".getBytes())
            )
        );
        MatcherAssert.assertThat(
            "Repository update was started when it does not supposed to",
            mvn.wasUpdated(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Upload events queue is empty", this.events.isEmpty());
    }

    @Test
    void returnsBadRequestOnIncorrectRequest() {
        MatcherAssert.assertThat(
            new PutMetadataChecksumSlice(
                this.asto, new ValidUpload.Dummy(), new Maven.Fake(this.asto),
                PutMetadataChecksumSliceTest.TEST_NAME, Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/any/request/line")
            )
        );
        MatcherAssert.assertThat("Upload events queue is empty", this.events.isEmpty());
    }

}
