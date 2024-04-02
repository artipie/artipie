/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.error.InvalidDigestException;
import com.google.common.base.Throwables;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Integration test for {@link AstoBlobs}.
 * @since 0.1
 */
final class AstoBlobsITCase {
    @Test
    void saveBlobDataAtCorrectPath() throws Exception {
        final InMemoryStorage storage = new InMemoryStorage();
        final AstoBlobs blobs = new AstoBlobs(
            new SubStorage(RegistryRoot.V2, storage),
            new Layout()
        );
        final byte[] bytes = new byte[]{0x00, 0x01, 0x02, 0x03};
        final Digest digest = blobs.put(new TrustedBlobSource(bytes))
            .toCompletableFuture().get().digest();
        MatcherAssert.assertThat(
            "Digest alg is not correct",
            digest.alg(), Matchers.equalTo("sha256")
        );
        final String hash = "054edec1d0211f624fed0cbca9d4f9400b0e491c43742af2c5b0abebf0c990d8";
        MatcherAssert.assertThat(
            "Digest sum is not correct",
            digest.hex(),
            Matchers.equalTo(hash)
        );
        MatcherAssert.assertThat(
            "File content is not correct",
            new BlockingStorage(storage).value(
                new Key.From(String.format("docker/registry/v2/blobs/sha256/05/%s/data", hash))
            ),
            Matchers.equalTo(bytes)
        );
    }

    @Test
    void failsOnDigestMismatch() {
        final InMemoryStorage storage = new InMemoryStorage();
        final AstoBlobs blobs = new AstoBlobs(
            storage, new Layout()
        );
        final String digest = "123";
        blobs.put(
            new CheckedBlobSource(new Content.From("data".getBytes()), new Digest.Sha256(digest))
        ).toCompletableFuture().handle(
            (blob, throwable) -> {
                MatcherAssert.assertThat(
                    "Exception thrown",
                    throwable,
                    new IsNot<>(new IsNull<>())
                );
                MatcherAssert.assertThat(
                    "Exception is InvalidDigestException",
                    Throwables.getRootCause(throwable),
                    new IsInstanceOf(InvalidDigestException.class)
                );
                MatcherAssert.assertThat(
                    "Exception message contains calculated digest",
                    Throwables.getRootCause(throwable).getMessage(),
                    new StringContains(
                        true,
                        "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"
                    )
                );
                MatcherAssert.assertThat(
                    "Exception message contains expected digest",
                    Throwables.getRootCause(throwable).getMessage(),
                    new StringContains(true, digest)
                );
                return CompletableFuture.allOf();
            }
        ).join();
    }

    @Test
    void writeAndReadBlob() throws Exception {
        final AstoBlobs blobs = new AstoBlobs(
            new InMemoryStorage(), new Layout()
        );
        final byte[] bytes = {0x05, 0x06, 0x07, 0x08};
        final Digest digest = blobs.put(new TrustedBlobSource(bytes))
            .toCompletableFuture().get().digest();
        final byte[] read = Flowable.fromPublisher(
            blobs.blob(digest)
                .toCompletableFuture().get()
                .get().content()
                .toCompletableFuture().get()
        ).toList().blockingGet().get(0).array();
        MatcherAssert.assertThat(read, Matchers.equalTo(bytes));
    }

    @Test
    void readAbsentBlob() throws Exception {
        final AstoBlobs blobs = new AstoBlobs(
            new InMemoryStorage(), new Layout()
        );
        final Digest digest = new Digest.Sha256(
            "0123456789012345678901234567890123456789012345678901234567890123"
        );
        MatcherAssert.assertThat(
            blobs.blob(digest).toCompletableFuture().get().isPresent(),
            new IsEqual<>(false)
        );
    }
}
