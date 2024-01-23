/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MetaUpdate.ByTgz}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MetaUpdateByTgzTest {
    /**
     * Storage.
     */
    private Storage asto;

    @BeforeEach
    void setUp() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void createsMetaFileWhenItNotExist() throws InterruptedException {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new BlockingStorage(this.asto).exists(new Key.From(prefix, "meta.json")),
            new IsEqual<>(true)
        );
    }

    @Test
    void updatesExistedMetaFile() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        new TestResource("storage/@hello/simple-npm-project/meta.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json().getJsonObject("versions").keySet(),
            Matchers.containsInAnyOrder("1.0.1", "1.0.2")
        );
    }

    @Test
    void metaContainsDistFields() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json()
                .getJsonObject("versions")
                .getJsonObject("1.0.2")
                .getJsonObject("dist")
                .keySet(),
            Matchers.containsInAnyOrder("integrity", "shasum", "tarball")
        );
    }

    @Test
    void containsCorrectLatestDistTag() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        new TestResource("storage/@hello/simple-npm-project/meta.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json()
                .getJsonObject("dist-tags")
                .getString("latest"),
            new IsEqual<>("1.0.2")
        );
    }

    private void updateByTgz(final Key prefix) {
        new MetaUpdate.ByTgz(
            new TgzArchive(
                new String(
                    new TestResource("binaries/simple-npm-project-1.0.2.tgz").asBytes(),
                    StandardCharsets.ISO_8859_1
                ), false
            )
        ).update(new Key.From(prefix), this.asto)
            .join();
    }
}
