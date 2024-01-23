/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.redis;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.ContentAs;
import com.artipie.asto.factory.StoragesLoader;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

/**
 * Tests for {@link RedisStorage}.
 *
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@DisabledOnOs(OS.WINDOWS)
public final class RedisStorageTest {
    /**
     * Redis test container.
     */
    private GenericContainer<?> redis;

    /**
     * Storage being tested.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.redis = new GenericContainer<>("redis:3-alpine")
            .withExposedPorts(6379);
        this.redis.start();
        this.storage = new StoragesLoader()
            .newObject("redis", config(this.redis.getFirstMappedPort()));
    }

    @Test
    @Timeout(1)
    void shouldNotBeBlockedByEndlessContent() throws Exception {
        final Key.From key = new Key.From("data");
        this.storage.save(
            key,
            new Content.From(
                ignored -> {
                }
            )
        );
        // @checkstyle MagicNumberCheck (1 line)
        TimeUnit.MILLISECONDS.sleep(100);
        MatcherAssert.assertThat(
            this.storage.exists(key).get(1, TimeUnit.SECONDS),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldUploadObjectWhenSave() {
        final byte[] data = "data2".getBytes();
        final String key = "a/b/c";
        this.storage.save(new Key.From(key), new Content.OneTime(new Content.From(data))).join();
        MatcherAssert.assertThat(
            this.download(key),
            Matchers.equalTo(data)
        );
    }

    @Test
    @Timeout(5)
    void shouldUploadObjectWhenSaveContentOfUnknownSize() {
        final byte[] data = "data?".getBytes();
        final String key = "unknown/size";
        this.storage.save(
            new Key.From(key),
            new Content.OneTime(new Content.From(data))
        ).join();
        MatcherAssert.assertThat(this.download(key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(15)
    void shouldUploadObjectWhenSaveLargeContent() throws ExecutionException, InterruptedException {
        final int size = 20 * 1024 * 1024;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        this.storage.save(
            new Key.From("big/data"),
            new Content.OneTime(new Content.From(data))
        ).join();
        MatcherAssert.assertThat(
            ContentAs.BYTES.apply(
                Single.just(this.storage.value(new Key.From("big/data")).join())
            ).toFuture().get(),
            Matchers.equalTo(data)
        );
    }

    @Test
    void shouldExistForSavedObject() {
        final byte[] data = "content".getBytes();
        final String key = "some/existing/key";
        this.save(key, data);
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).exists(new Key.From(key)),
            Matchers.equalTo(true)
        );
    }

    @Test
    void shouldListKeysInOrder() {
        final byte[] data = "some data!".getBytes();
        Arrays.asList(
            new Key.From("1"),
            new Key.From("a", "b", "c", "1"),
            new Key.From("a", "b", "2"),
            new Key.From("a", "z"),
            new Key.From("z")
        ).forEach(
            key -> this.save(key.string(), data)
        );
        final Collection<String> keys = new BlockingStorage(this.storage)
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
    void shouldGetObjectWhenLoad() {
        final byte[] data = "data".getBytes();
        final String key = "some/key";
        this.save(key, data);
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldCopyObjectWhenMoved() {
        final byte[] original = "something".getBytes();
        final String source = "source";
        this.save(source, original);
        final String destination = "destination";
        new BlockingStorage(this.storage).move(
            new Key.From(source),
            new Key.From(destination)
        );
        MatcherAssert.assertThat(
            this.download(destination),
            new IsEqual<>(original)
        );
    }

    @Test
    void shouldDeleteOriginalObjectWhenMoved() {
        final String source = "src";
        this.save(source, "some data".getBytes());
        new BlockingStorage(this.storage).move(
            new Key.From(source),
            new Key.From("dest")
        );
        MatcherAssert.assertThat(
            this.download(source),
            Matchers.nullValue()
        );
    }

    @Test
    void shouldDeleteObject() {
        final byte[] data = "to be deleted".getBytes();
        final String key = "to/be/deleted";
        this.save(key, data);
        new BlockingStorage(this.storage).delete(new Key.From(key));
        MatcherAssert.assertThat(
            this.download(key),
            Matchers.nullValue()
        );
    }

    @Test
    void readMetadata() {
        final String key = "random/data";
        this.save(key, "random data".getBytes());
        final Meta meta = this.storage.metadata(new Key.From(key)).join();
        MatcherAssert.assertThat(
            "size",
            meta.read(Meta.OP_SIZE).get(),
            new IsEqual<>(11L)
        );
    }

    @Test
    void returnsIdentifier() {
        MatcherAssert.assertThat(
            this.storage.identifier(),
            Matchers.stringContainsInOrder("Radis", "id=")
        );
    }

    private static com.artipie.asto.factory.Config config(final Integer port) {
        return new com.artipie.asto.factory.Config.YamlStorageConfig(
            Yaml.createYamlMappingBuilder().add("type", "redis")
                .add(
                    "config",
                    Yaml.createYamlMappingBuilder()
                        .add(
                            "singleServerConfig",
                            Yaml.createYamlMappingBuilder()
                                .add(
                                    "address",
                                    String.format("redis://127.0.0.1:%d", port)
                                ).build()
                        ).build()
                ).build()
        );
    }

    private byte[] download(final String key) {
        try {
            final Map<String, byte[]> map = Redisson.create(
                Config.fromYAML(
                    config(this.redis.getFirstMappedPort()).config("config").toString()
                )
            ).getMap(RedisStorageFactory.DEF_OBJ_NAME);
            return map.get(key);
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }

    private void save(final String key, final byte[] data) {
        try {
            final Map<String, byte[]> map = Redisson.create(
                Config.fromYAML(
                    config(this.redis.getFirstMappedPort()).config("config").toString()
                )
            ).getMap(RedisStorageFactory.DEF_OBJ_NAME);
            map.put(key, data);
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
