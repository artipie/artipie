/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.etcd;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.github.dockerjava.api.DockerClient;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.launcher.EtcdContainer;
import io.etcd.jetcd.test.EtcdClusterExtension;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.DockerClientFactory;

/**
 * Test case for etcd-storage.
 * @since 1.0
 * @todo #309:30min Run Etcd in windows containers while testing on windows.
 *  Currently, when we try to run integration tests based on testcontainers within a platform
 *  windows, we notice that Etcd container (presently based on Linux) doesn't work. We have to build
 *  and publish an Etcd docker image based on windows to avoid this issue.
 *  Please, build an Etcd image for windows (version 3.5.1) and write tests so as to detect before
 *  running integration tests, the type of platform (linux or windows) in order to pull the right
 *  docker image. After that, enable the test below for windows by removing
 *  {@code @DisabledOnOs(OS.WINDOWS)}.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class EtcdStorageITCase {

    /**
     * Test cluster.
     */
    static final EtcdClusterExtension ETCD = new EtcdClusterExtension(
        "test-etcd",
        1,
        false,
        "--data-dir",
        "/data.etcd0"
    );

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        final DockerClient client = DockerClientFactory.instance().client();
        client.pullImageCmd(EtcdContainer.ETCD_DOCKER_IMAGE_NAME)
            .start()
            .awaitCompletion();
        ETCD.start();
    }

    @BeforeEach
    void setUp() {
        final List<URI> endpoints = ETCD.getClientEndpoints();
        this.storage = new EtcdStorage(
            Client.builder().endpoints(endpoints).build(),
            endpoints.stream().map(URI::toString).collect(Collectors.joining())
        );
    }

    @AfterAll
    static void afterAll() {
        ETCD.close();
    }

    @Test
    void listsItems() {
        final Key one = new Key.From("one");
        final Key two = new Key.From("a/two");
        final Key three = new Key.From("a/three");
        this.storage.save(
            one,
            new Content.From("data 1".getBytes(StandardCharsets.UTF_8))
        ).join();
        this.storage.save(
            two,
            new Content.From("data 2".getBytes(StandardCharsets.UTF_8))
        ).join();
        this.storage.save(
            three,
            new Content.From("data 3".getBytes(StandardCharsets.UTF_8))
        ).join();
        MatcherAssert.assertThat(
            "Should list all items",
            new BlockingStorage(this.storage).list(Key.ROOT),
            Matchers.hasItems(one, two, three)
        );
        MatcherAssert.assertThat(
            "Should list prefixed items",
            new BlockingStorage(this.storage).list(new Key.From("a")),
            Matchers.hasItems(two, three)
        );
    }

    @Test
    void readAndWrite() {
        final Key key = new Key.From("one", "two", "three");
        final byte[] data = "some binary data".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, "first revision".getBytes());
        bsto.save(key, "second revision".getBytes());
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.value(key), Matchers.equalTo(data));
    }

    @Test
    @SuppressWarnings("deprecation")
    void getSize() {
        final Key key = new Key.From("another", "key");
        final byte[] data = "data with size".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.size(key), Matchers.equalTo((long) data.length));
    }

    @Test
    void checkExist() {
        final Key key = new Key.From("existing", "item");
        final byte[] data = "I exist".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.exists(key), Matchers.is(true));
    }

    @Test
    void move() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key src = new Key.From("source");
        final Key dst = new Key.From("destination");
        final byte[] data = "data to move".getBytes();
        bsto.save(src, data);
        bsto.move(src, dst);
        MatcherAssert.assertThat("source still exist", bsto.exists(src), new IsEqual<>(false));
        MatcherAssert.assertThat("source was not moved", bsto.value(dst), new IsEqual<>(data));
    }

    @Test
    void delete() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("temporary");
        final byte[] data = "data to delete".getBytes();
        bsto.save(key, data);
        bsto.delete(key);
        MatcherAssert.assertThat(bsto.exists(key), new IsEqual<>(false));
    }

    @Test
    void failsIfNothingToDelete() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("nothing");
        final CompletionException cex = Assertions.assertThrows(
            CompletionException.class,
            () -> bsto.delete(key)
        );
        MatcherAssert.assertThat(
            cex.getCause().getCause().getMessage(),
            new IsEqual<>(String.format("No value for key: %s", key))
        );
    }

    @Test
    void returnsIdentifier() {
        MatcherAssert.assertThat(
            this.storage.identifier(),
            Matchers.stringContainsInOrder(
                "Etcd",
                ETCD.getClientEndpoints().stream().map(URI::toString).collect(Collectors.joining())
            )
        );
    }
}
