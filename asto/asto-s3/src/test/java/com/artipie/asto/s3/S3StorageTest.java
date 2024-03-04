/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.google.common.io.ByteStreams;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests for {@link S3Storage}.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
@DisabledOnOs(OS.WINDOWS)
class S3StorageTest {
    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket to use in tests.
     */
    private String bucket;

    @BeforeEach
    void setUp(final AmazonS3 client) {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
    }

    @Test
    void shouldUploadObjectWhenSave(final AmazonS3 client) throws Exception {
        final byte[] data = "data2".getBytes();
        final String key = "a/b/c";
        this.storage().save(new Key.From(key), new Content.OneTime(new Content.From(data))).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(5)
    void shouldUploadObjectWhenSaveContentOfUnknownSize(final AmazonS3 client) throws Exception {
        final byte[] data = "data?".getBytes();
        final String key = "unknown/size";
        this.storage().save(
            new Key.From(key),
            new Content.OneTime(new Content.From(data))
        ).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(15)
    void shouldUploadObjectWhenSaveLargeContent(final AmazonS3 client) throws Exception {
        final int size = 20 * 1024 * 1024;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final String key = "big/data";
        this.storage().save(
            new Key.From(key),
            new Content.OneTime(new Content.From(data))
        ).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(150)
    void shouldUploadLargeChunkedContent(final AmazonS3 client) throws Exception {
        final String key = "big/data";
        final int s3MinPartSize = 5 * 1024 * 1024;
        final int s3MinMultipart = 10 * 1024 * 1024;
        final int totalSize = s3MinMultipart * 2;
        final Random rnd = new Random();
        final byte[] sentData = new byte[totalSize];
        final Callable<Flowable<ByteBuffer>> getGenerator = () -> Flowable.generate(
            AtomicInteger::new,
            (counter, output) -> {
                final int sent = counter.get();
                final int sz = Math.min(totalSize - sent, rnd.nextInt(s3MinPartSize));
                counter.getAndAdd(sz);
                if (sent < totalSize) {
                    final byte[] data = new byte[sz];
                    rnd.nextBytes(data);
                    for (int i = 0, j = sent; i < data.length; ++i, ++j) {
                        sentData[j] = data[i];
                    }
                    output.onNext(ByteBuffer.wrap(data));
                } else {
                    output.onComplete();
                }
                return counter;
            });
        MatcherAssert.assertThat("Generator results mismatch",
            new PublisherAs(new Content.From(getGenerator.call())).bytes()
                .toCompletableFuture().join(),
            Matchers.equalTo(sentData)
        );
        this.storage().save(new Key.From(key), new Content.From(getGenerator.call())).join();
        MatcherAssert.assertThat("Saved results mismatch (S3 client)",
            this.download(client, key), Matchers.equalTo(sentData)
        );
        MatcherAssert.assertThat("Saved results mismatch (S3Storage)",
            new PublisherAs(this.storage().value(new Key.From(key)).toCompletableFuture().get())
                .bytes().toCompletableFuture().join(), Matchers.equalTo(sentData)
        );
    }

    @Test
    void shouldAbortMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        this.storage().save(
            new Key.From("abort"),
            new Content.OneTime(new Content.From(Flowable.error(new IllegalStateException())))
        ).exceptionally(ignore -> null).join();
        final List<MultipartUpload> uploads = client.listMultipartUploads(
            new ListMultipartUploadsRequest(this.bucket)
        ).getMultipartUploads();
        MatcherAssert.assertThat(uploads, new IsEmptyIterable<>());
    }

    @Test
    void shouldExistForSavedObject(final AmazonS3 client) throws Exception {
        final byte[] data = "content".getBytes();
        final String key = "some/existing/key";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final boolean exists = new BlockingStorage(this.storage())
            .exists(new Key.From(key));
        MatcherAssert.assertThat(
            exists,
            Matchers.equalTo(true)
        );
    }

    @Test
    void shouldListKeysInOrder(final AmazonS3 client) throws Exception {
        final byte[] data = "some data!".getBytes();
        Arrays.asList(
            new Key.From("1"),
            new Key.From("a", "b", "c", "1"),
            new Key.From("a", "b", "2"),
            new Key.From("a", "z"),
            new Key.From("z")
        ).forEach(
            key -> client.putObject(
                this.bucket,
                key.string(),
                new ByteArrayInputStream(data),
                new ObjectMetadata()
            )
        );
        final Collection<String> keys = new BlockingStorage(this.storage())
            .list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }

    @Test
    void shouldGetObjectWhenLoad(final AmazonS3 client) throws Exception {
        final byte[] data = "data".getBytes();
        final String key = "some/key";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final byte[] value = new BlockingStorage(this.storage())
            .value(new Key.From(key));
        MatcherAssert.assertThat(
            value,
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldCopyObjectWhenMoved(final AmazonS3 client) throws Exception {
        final byte[] original = "something".getBytes();
        final String source = "source";
        client.putObject(
            this.bucket,
            source, new ByteArrayInputStream(original),
            new ObjectMetadata()
        );
        final String destination = "destination";
        new BlockingStorage(this.storage()).move(
            new Key.From(source),
            new Key.From(destination)
        );
        try (S3Object s3Object = client.getObject(this.bucket, destination)) {
            MatcherAssert.assertThat(
                ByteStreams.toByteArray(s3Object.getObjectContent()),
                new IsEqual<>(original)
            );
        }
    }

    @Test
    void shouldDeleteOriginalObjectWhenMoved(final AmazonS3 client) throws Exception {
        final String source = "src";
        client.putObject(
            this.bucket,
            source,
            new ByteArrayInputStream("some data".getBytes()),
            new ObjectMetadata()
        );
        new BlockingStorage(this.storage()).move(
            new Key.From(source),
            new Key.From("dest")
        );
        MatcherAssert.assertThat(
            client.doesObjectExist(this.bucket, source),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldDeleteObject(final AmazonS3 client) throws Exception {
        final byte[] data = "to be deleted".getBytes();
        final String key = "to/be/deleted";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        new BlockingStorage(this.storage()).delete(new Key.From(key));
        MatcherAssert.assertThat(
            client.doesObjectExist(this.bucket, key),
            new IsEqual<>(false)
        );
    }

    @Test
    void readMetadata(final AmazonS3 client) throws Exception {
        final String key = "random/data";
        client.putObject(
            this.bucket, key,
            new ByteArrayInputStream("random data".getBytes()), new ObjectMetadata()
        );
        final Meta meta = this.storage().metadata(new Key.From(key)).join();
        MatcherAssert.assertThat(
            "size",
            meta.read(Meta.OP_SIZE).get(),
            new IsEqual<>(11L)
        );
        MatcherAssert.assertThat(
            "MD5",
            meta.read(Meta.OP_MD5).get(),
            new IsEqual<>("3e58b24739a19c3e2e1b21bac818c6cd")
        );
    }

    @Test
    void returnsIdentifier() {
        MatcherAssert.assertThat(
            this.storage().identifier(),
            Matchers.stringContainsInOrder(
                "S3", "http://localhost", String.valueOf(MOCK.getHttpPort()), this.bucket
            )
        );
    }

    private byte[] download(final AmazonS3 client, final String key) throws IOException {
        try (S3Object s3Object = client.getObject(this.bucket, key)) {
            return ByteStreams.toByteArray(s3Object.getObjectContent());
        }
    }

    private Storage storage() {
        return new StoragesLoader()
            .newObject(
                "s3",
                new Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "us-east-1")
                        .add("bucket", this.bucket)
                        .add("endpoint", String.format("http://localhost:%d", MOCK.getHttpPort()))
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "foo")
                                .add("secretAccessKey", "bar")
                                .build()
                        )
                        .build()
                )
            );
    }
}
