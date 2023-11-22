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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository#addJson(Content, Optional)}.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class AstoRepositoryAddJsonTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Package pack;

    /**
     * Version of package.
     */
    private String version;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.pack = new JsonPackage(
            new Content.From(
                new TestResource("minimal-package.json").asBytes()
            )
        );
        this.version = this.pack.version(Optional.empty())
            .toCompletableFuture().join()
            .get();
    }

    @Test
    void shouldAddPackageToAll() throws Exception {
        this.addJsonToAsto(this.packageJson(), Optional.empty());
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject(name.string())
                .keySet(),
            new IsEqual<>(new SetOf<>(this.version))
        );
    }

    @Test
    void shouldAddPackageToAllWhenOtherVersionExists() throws Exception {
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            "{\"packages\":{\"vendor/package\":{\"2.0\":{}}}}".getBytes()
        );
        this.addJsonToAsto(this.packageJson(), Optional.empty());
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject("vendor/package")
                .keySet(),
            new IsEqual<>(new SetOf<>("2.0", this.version))
        );
    }

    @Test
    void shouldAddPackage() throws Exception {
        this.addJsonToAsto(this.packageJson(), Optional.empty());
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Package with correct version should present in packages after being added",
            this.packages(name.key()).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>(this.version))
        );
    }

    @Test
    void shouldAddPackageWhenOtherVersionExists() throws Exception {
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        new BlockingStorage(this.storage).save(
            name.key(),
            "{\"packages\":{\"vendor/package\":{\"1.1.0\":{}}}}".getBytes()
        );
        this.addJsonToAsto(this.packageJson(), Optional.empty());
        MatcherAssert.assertThat(
            // @checkstyle LineLengthCheck (1 line)
            "Package with both new and old versions should present in packages after adding new version",
            this.packages(name.key()).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>("1.1.0", this.version))
        );
    }

    @Test
    void shouldDeleteSourceAfterAdding() throws Exception {
        this.addJsonToAsto(this.packageJson(), Optional.empty());
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join().stream()
                .map(Key::string)
                .collect(Collectors.toList()),
            Matchers.contains("packages.json", "vendor/package.json")
        );
    }

    @Test
    void shouldAddPackageWithoutVersionWithPassedValue() {
        final Optional<String> vers = Optional.of("2.3.4");
        this.addJsonToAsto(
            new Content.From(new TestResource("package-without-version.json").asBytes()),
            vers
        );
        final Name name = new Name("vendor/package");
        final JsonObject pkgs = this.packages(name.key())
            .getJsonObject(name.string());
        MatcherAssert.assertThat(
            "Packages contains package with added version",
            pkgs.keySet(),
            new IsEqual<>(new SetOf<>(vers.get()))
        );
        MatcherAssert.assertThat(
            "Added package contains `version` entry",
            pkgs.getJsonObject(vers.get()).getString("version"),
            new IsEqual<>(vers.get())
        );
    }

    @Test
    void shouldFailToAddPackageWithoutVersion() {
        final CompletionException result = Assertions.assertThrows(
            CompletionException.class,
            () -> this.addJsonToAsto(
                new Content.From(new TestResource("package-without-version.json").asBytes()),
                Optional.empty()
            )
        );
        MatcherAssert.assertThat(
            result.getCause(),
            new IsInstanceOf(IllegalStateException.class)
        );
    }

    private JsonObject packages(final Key key) {
        final JsonObject saved;
        final byte[] bytes = new BlockingStorage(this.storage).value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            saved = reader.readObject();
        }
        return saved.getJsonObject("packages");
    }

    private void addJsonToAsto(final Content json, final Optional<String> vers) {
        new AstoRepository(this.storage)
            .addJson(json, vers)
            .join();
    }

    private Content packageJson() throws Exception {
        final byte[] bytes;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        writer.writeObject(this.pack.json().toCompletableFuture().join());
        out.flush();
        bytes = out.toByteArray();
        writer.close();
        return new Content.From(bytes);
    }
}
