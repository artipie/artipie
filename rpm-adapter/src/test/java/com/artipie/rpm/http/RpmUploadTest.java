/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.TestRpm;
import com.artipie.scheduling.ArtifactEvent;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmUpload}.
 *
 * @since 0.8.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RpmUploadTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void canUploadArtifact() throws Exception {
        final byte[] content = Files.readAllBytes(new TestRpm.Abc().path());
        final Optional<Queue<ArtifactEvent>> events = Optional.of(new LinkedList<>());
        MatcherAssert.assertThat(
            "ACCEPTED 202 returned",
            new RpmUpload(this.storage, new RepoConfig.Simple(), events).response(
                new RequestLine("PUT", "/uploaded.rpm"),
                new ListOf<Map.Entry<String, String>>(),
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            "Content saved to storage",
            new BlockingStorage(this.storage).value(new Key.From("uploaded.rpm")),
            new IsEqual<>(content)
        );
        MatcherAssert.assertThat(
            "Metadata updated",
            new BlockingStorage(this.storage).list(new Key.From("repodata")).isEmpty(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Events queue has one item", events.get().size() == 1);
    }

    @Test
    void canReplaceArtifact() throws Exception {
        final byte[] content = Files.readAllBytes(new TestRpm.Abc().path());
        final Key key = new Key.From("replaced.rpm");
        new BlockingStorage(this.storage).save(key, "uploaded package".getBytes());
        MatcherAssert.assertThat(
            new RpmUpload(this.storage, new RepoConfig.Simple(), Optional.empty()).response(
                new RequestLine("PUT", "/replaced.rpm?override=true"),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(key),
            new IsEqual<>(content)
        );
    }

    @Test
    void dontReplaceArtifact() throws Exception {
        final byte[] content =
            "first package content".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("not-replaced.rpm");
        final Optional<Queue<ArtifactEvent>> events = Optional.of(new LinkedList<>());
        new BlockingStorage(this.storage).save(key, content);
        MatcherAssert.assertThat(
            new RpmUpload(this.storage, new RepoConfig.Simple(), events).response(
                new RequestLine("PUT", "/not-replaced.rpm"),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap("second package content".getBytes()))
            ),
            new RsHasStatus(RsStatus.CONFLICT)
        );
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(key),
            new IsEqual<>(content)
        );
        MatcherAssert.assertThat("Events queue is empty", events.get().size() == 0);
    }

    @Test
    void skipsUpdateWhenParamSkipIsTrue() throws Exception {
        final byte[] content = Files.readAllBytes(new TestRpm.Abc().path());
        MatcherAssert.assertThat(
            "ACCEPTED 202 returned",
            new RpmUpload(this.storage, new RepoConfig.Simple(), Optional.empty()).response(
                new RequestLine("PUT", "/my-package.rpm?skip_update=true"),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            "Content saved to storage",
            new BlockingStorage(this.storage)
                .value(new Key.From(RpmUpload.TO_ADD, "my-package.rpm")),
            new IsEqual<>(content)
        );
        MatcherAssert.assertThat(
            "Metadata not updated",
            new BlockingStorage(this.storage).list(new Key.From("repodata")).isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void skipsUpdateIfModeIsCron() throws Exception {
        final byte[] content = Files.readAllBytes(new TestRpm.Abc().path());
        MatcherAssert.assertThat(
            "ACCEPTED 202 returned",
            new RpmUpload(
                this.storage, new RepoConfig.Simple(RepoConfig.UpdateMode.CRON), Optional.empty()
            ).response(
                new RequestLine("PUT", "/abc-package.rpm"),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            "Content saved to temp location",
            new BlockingStorage(this.storage)
                .value(new Key.From(RpmUpload.TO_ADD, "abc-package.rpm")),
            new IsEqual<>(content)
        );
        MatcherAssert.assertThat(
            "Metadata not updated",
            new BlockingStorage(this.storage).list(new Key.From("repodata")).isEmpty(),
            new IsEqual<>(true)
        );
    }
}
