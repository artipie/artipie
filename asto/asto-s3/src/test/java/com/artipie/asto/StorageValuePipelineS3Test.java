package com.artipie.asto;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.s3.S3Storage;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.asto.test.ReadWithDelaysStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Test for {@link StorageValuePipeline} backed by {@link S3Storage}
 */
public class StorageValuePipelineS3Test {

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
        asto = new StoragesLoader()
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

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void processesExistingItemAndReturnsResult() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(key, new Content.From("five\nsix\neight".getBytes(charset))).join();
        MatcherAssert.assertThat(
            "Resulting lines count should be 4",
            new StorageValuePipeline<Integer>(this.asto, key).processWithResult(
                (input, out) -> {
                    try {
                        final List<String> list = IOUtils.readLines(input.get(), charset);
                        list.add(2, "seven");
                        IOUtils.writeLines(list, "\n", out, charset);
                        return list.size();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            ).toCompletableFuture().join(),
            new IsEqual<>(4)
        );
        MatcherAssert.assertThat(
            "Storage item was not updated",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("five\nsix\nseven\neight\n")
        );
    }

    @Test
    void processesExistingItemAndReturnsResultWithThen() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;

        MatcherAssert.assertThat(
            "Resulting lines count should be 4",
            this.asto.save(key, new Content.From("five\nsix\neight".getBytes(charset))).thenCompose(unused -> {
                return new StorageValuePipeline<Integer>(this.asto, key).processWithResult(
                    (input, out) -> {
                        try {
                            final List<String> list = IOUtils.readLines(input.get(), charset);
                            list.add(2, "seven");
                            IOUtils.writeLines(list, "\n", out, charset);
                            return list.size();
                        } catch (final IOException err) {
                            throw new ArtipieIOException(err);
                        }
                    }
                );
            }).join(),
            new IsEqual<>(4)
        );
        MatcherAssert.assertThat(
            "Storage item was not updated",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("five\nsix\nseven\neight\n")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "key_from,key_to",
        "key_from,key_from"
    })
    void processesExistingLargeSizeItem(
        final String read, final String write
    ) {
        final int size = 1024 * 1024;
        final int bufsize = 128;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final Key kfrom = new Key.From(read);
        final Key kto = new Key.From(write);
        this.asto.save(kfrom, new Content.From(data)).join();
        new StorageValuePipeline<String>(new ReadWithDelaysStorage(this.asto), kfrom, kto)
            .processWithResult(
                (input, out) -> {
                    final byte[] buffer = new byte[bufsize];
                    try {
                        final InputStream stream = input.get();
                        while (stream.read(buffer) != -1) {
                            IOUtils.write(buffer, out);
                            out.flush();
                        }
                        new Random().nextBytes(buffer);
                        IOUtils.write(buffer, out);
                        out.flush();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                    return "res";
                }
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.asto).value(kto).length,
            new IsEqual<>(size + bufsize)
        );
    }
}
