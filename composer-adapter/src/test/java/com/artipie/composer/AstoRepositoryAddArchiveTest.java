/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.http.Archive;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.util.Optional;

/**
 * Tests for {@link AstoRepository#addArchive(Archive, Content)}.
 */
final class AstoRepositoryAddArchiveTest {
    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Content archive;

    /**
     * Archive name.
     */
    private Archive.Name name;

    @BeforeEach
    void init() {
        final String zip = "log-1.1.3.zip";
        this.storage = new InMemoryStorage();
        this.archive = new Content.From(
            new TestResource(zip).asBytes()
        );
        this.name = new Archive.Name(zip, "1.1.3");
    }

    @Test
    void shouldAddPackageToAll() {
        this.saveZipArchive();
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject("psr/log")
                .keySet(),
            new IsEqual<>(new SetOf<>(this.name.version()))
        );
    }

    @Test
    void shouldAddPackageToAllWhenOtherVersionExists() {
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            "{\"packages\":{\"psr/log\":{\"1.1.2\":{}}}}".getBytes()
        );
        this.saveZipArchive();
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject("psr/log")
                .keySet(),
            new IsEqual<>(new SetOf<>("1.1.2", this.name.version()))
        );
    }

    @Test
    void shouldAddArchive() {
        this.saveZipArchive();
        Assertions.assertTrue(
            this.storage.exists(new Key.From("artifacts", this.name.full()))
                .toCompletableFuture().join()
        );
    }

    private void saveZipArchive() {
        new AstoRepository(this.storage, Optional.of("http://artipie:8080/"))
            .addArchive(
                new Archive.Zip(this.name),
                this.archive
            ).join();
    }

    private JsonObject packages(final Key key) {
        return this.storage.value(key).join()
            .asJsonObject()
            .getJsonObject("packages");
    }
}
