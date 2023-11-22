/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Blob;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.error.InvalidManifestException;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link AstoManifests}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AstoManifestsTest {

    /**
     * Blobs used in tests.
     */
    private AstoBlobs blobs;

    /**
     * Repository manifests being tested.
     */
    private AstoManifests manifests;

    @BeforeEach
    void setUp() {
        final Storage storage = new ExampleStorage();
        final Layout layout = new DefaultLayout();
        final RepoName name = new RepoName.Simple("my-alpine");
        this.blobs = new AstoBlobs(storage, layout, name);
        this.manifests = new AstoManifests(storage, this.blobs, layout, name);
    }

    @Test
    @Timeout(5)
    void shouldReadManifest() {
        final ManifestRef ref = new ManifestRef.FromTag(new Tag.Valid("1"));
        final byte[] manifest = this.manifest(ref);
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(manifest.length, Matchers.equalTo(528));
    }

    @Test
    @Timeout(5)
    void shouldReadNoManifestIfAbsent() throws Exception {
        final Optional<Manifest> manifest = this.manifests.get(
            new ManifestRef.FromTag(new Tag.Valid("2"))
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(manifest.isPresent(), new IsEqual<>(false));
    }

    @Test
    @Timeout(5)
    void shouldReadAddedManifest() {
        final Blob config = this.blobs.put(new TrustedBlobSource("config".getBytes()))
            .toCompletableFuture().join();
        final Blob layer = this.blobs.put(new TrustedBlobSource("layer".getBytes()))
            .toCompletableFuture().join();
        final byte[] data = this.getJsonBytes(config, layer, "my-type");
        final ManifestRef ref = new ManifestRef.FromTag(new Tag.Valid("some-tag"));
        final Manifest manifest = this.manifests.put(ref, new Content.From(data))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(this.manifest(ref), new IsEqual<>(data));
        MatcherAssert.assertThat(
            this.manifest(new ManifestRef.FromDigest(manifest.digest())),
            new IsEqual<>(data)
        );
    }

    @Test
    @Timeout(5)
    void shouldFailPutManifestIfMediaTypeIsEmpty() {
        final Blob config = this.blobs.put(new TrustedBlobSource("config".getBytes()))
            .toCompletableFuture().join();
        final Blob layer = this.blobs.put(new TrustedBlobSource("layer".getBytes()))
            .toCompletableFuture().join();
        final byte[] data = this.getJsonBytes(config, layer, "");
        final CompletionStage<Manifest> future = this.manifests.put(
            new ManifestRef.FromTag(new Tag.Valid("ddd")),
            new Content.From(data)
        );
        final CompletionException exception = Assertions.assertThrows(
            CompletionException.class,
            () -> future.toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            "Exception cause should be instance of InvalidManifestException",
            exception.getCause(),
            new IsInstanceOf(InvalidManifestException.class)
        );
        MatcherAssert.assertThat(
            "Exception does not contain expected message",
            exception.getMessage(),
            new StringContains("Required field `mediaType` is empty")
        );
    }

    @Test
    @Timeout(5)
    void shouldFailPutInvalidManifest() {
        final CompletionStage<Manifest> future = this.manifests.put(
            new ManifestRef.FromTag(new Tag.Valid("ttt")),
            Content.EMPTY
        );
        final CompletionException exception = Assertions.assertThrows(
            CompletionException.class,
            () -> future.toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            exception.getCause(),
            new IsInstanceOf(InvalidManifestException.class)
        );
    }

    @Test
    @Timeout(5)
    void shouldReadTags() {
        final Tags tags = this.manifests.tags(Optional.empty(), Integer.MAX_VALUE)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(tags.json()).asciiString().toCompletableFuture().join(),
            new IsEqual<>("{\"name\":\"my-alpine\",\"tags\":[\"1\",\"latest\"]}")
        );
    }

    private byte[] manifest(final ManifestRef ref) {
        return this.manifests.get(ref)
            .thenApply(Optional::get)
            .thenCompose(mnf -> new PublisherAs(mnf.content()).bytes())
            .toCompletableFuture().join();
    }

    private byte[] getJsonBytes(final Blob config, final Blob layer, final String mtype) {
        return Json.createObjectBuilder()
            .add(
                "config",
                Json.createObjectBuilder().add("digest", config.digest().string())
            )
            .add("mediaType", mtype)
            .add(
                "layers",
                Json.createArrayBuilder()
                    .add(
                        Json.createObjectBuilder().add("digest", layer.digest().string())
                    )
                    .add(
                        Json.createObjectBuilder()
                            .add("digest", "sha256:123")
                            .add("urls", Json.createArrayBuilder().add("https://artipie.com/"))
                    )
            )
            .build().toString().getBytes();
    }
}
