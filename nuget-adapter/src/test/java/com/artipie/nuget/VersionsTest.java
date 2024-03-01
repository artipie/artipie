/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.metadata.NuspecField;
import com.artipie.nuget.metadata.Version;
import org.cactoos.io.ReaderOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for {@link Versions}.
 */
class VersionsTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void shouldAddVersionWhenEmpty() throws Exception {
        final Version version = new Version("0.1.0");
        final List<String> versions = this.addVersionTo("{\"versions\":[]}", version);
        MatcherAssert.assertThat(
            versions,
            Matchers.equalTo(Collections.singletonList(version.normalized()))
        );
    }

    @Test
    void shouldAddVersionWhenNotEmpty() throws Exception {
        final Version version = new Version("1.1.0");
        final List<String> versions = this.addVersionTo("{\"versions\":[\"1.0.0\"]}", version);
        MatcherAssert.assertThat(
            versions,
            Matchers.equalTo(Arrays.asList("1.0.0", version.normalized()))
        );
    }

    @Test
    void shouldGetAllVersionsWhenEmpty() {
        final Versions versions = new Versions(
            Json.createReader(new ReaderOf("{ \"versions\":[] }")).readObject()
        );
        MatcherAssert.assertThat(versions.all(), new IsEmptyCollection<>());
    }

    @Test
    void shouldGetAllVersionsOrdered() {
        final Versions versions = new Versions(
            Json.createReader(
                new ReaderOf("{ \"versions\":[\"1.0.1\",\"0.1\",\"2.0\",\"1.0\"] }")
            ).readObject()
        );
        MatcherAssert.assertThat(
            versions.all().stream().map(NuspecField::normalized).collect(Collectors.toList()),
            new IsEqual<>(Arrays.asList("0.1", "1.0", "1.0.1", "2.0"))
        );
    }

    @Test
    void shouldSave() {
        final Key.From key = new Key.From("foo");
        final JsonObject data = Json.createObjectBuilder().build();
        new Versions(data).save(this.storage, key).toCompletableFuture().join();
        Assertions.assertEquals(
            data.toString(),
            this.storage.value(key).join().asString(),
            "Saved versions are not identical to versions initial content"
        );
    }

    @Test
    void shouldSaveEmpty() throws Exception {
        final Key.From key = new Key.From("bar");
        new Versions().save(this.storage, key).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Versions created from scratch expected to be empty",
            this.versions(key),
            new IsEmptyCollection<>()
        );
    }

    private List<String> addVersionTo(final String original, final Version version)
        throws Exception {
        final Versions versions = new Versions(
            Json.createReader(new ReaderOf(original)).readObject()
        );
        final Key.From sink = new Key.From("sink");
        versions.add(version).save(this.storage, sink).toCompletableFuture().join();
        return this.versions(sink);
    }

    private List<String> versions(final Key key) throws Exception {
        final byte[] bytes = new BlockingStorage(this.storage).value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            return reader.readObject()
                .getJsonArray("versions")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(Collectors.toList());
        }
    }
}
