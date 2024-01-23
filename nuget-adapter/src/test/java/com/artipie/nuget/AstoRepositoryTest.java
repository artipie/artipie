/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.metadata.PackageId;
import com.artipie.nuget.metadata.Version;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Every;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository}.
 *
 * @since 0.5
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidCatchingGenericException"})
class AstoRepositoryTest {

    /**
     * Storage used in tests.
     */
    private Storage asto;

    /**
     * Blocking storage used in tests.
     */
    private BlockingStorage storage;

    /**
     * Repository to test.
     */
    private AstoRepository repository;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.storage = new BlockingStorage(this.asto);
        this.repository = new AstoRepository(this.asto);
    }

    @Test
    void shouldAddPackage() throws Exception {
        final Key.From source = new Key.From("package.zip");
        this.repository.add(new Content.From(this.nupkg().bytes())).toCompletableFuture().join();
        final PackageId id = new PackageId("newtonsoft.json");
        final String version = "12.0.3";
        final PackageIdentity identity = new PackageIdentity(id, new Version(version));
        MatcherAssert.assertThat(
            this.storage.value(identity.nupkgKey()),
            Matchers.equalTo(this.nupkg().bytes())
        );
        MatcherAssert.assertThat(
            new String(
                this.storage.value(identity.hashKey())
            ),
            Matchers.equalTo(
                "aTRmXwR5xYu+mWxE8r8W1DWnL02SeV8LwdQMsLwTWP8OZgrCCyTqvOAe5hRb1VNQYXjln7qr0PKpSyO/pcc19Q=="
            )
        );
        final String nuspec = "newtonsoft.json.nuspec";
        MatcherAssert.assertThat(
            this.storage.value(identity.nuspecKey()),
            new IsEqual<>(new NewtonJsonResource(nuspec).bytes())
        );
        MatcherAssert.assertThat(
            this.versions(new PackageKeys(id).versionsKey()),
            Matchers.contains(version)
        );
        MatcherAssert.assertThat(
            this.storage.exists(source),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldFailToAddInvalidPackage() {
        final Throwable cause = Assertions.assertThrows(
            CompletionException.class,
            () -> this.repository.add(new Content.From("not a zip".getBytes()))
                .toCompletableFuture().join(),
            "Repository expected to throw InvalidPackageException if package is invalid and cannot be added"
        ).getCause();
        MatcherAssert.assertThat(
            cause,
            new IsInstanceOf(InvalidPackageException.class)
        );
    }

    @Test
    void shouldGetPackageVersions() throws Exception {
        final byte[] bytes = "{\"versions\":[\"1.0.0\",\"1.0.1\"]}"
            .getBytes(StandardCharsets.US_ASCII);
        final PackageKeys foo = new PackageKeys("Foo");
        this.storage.save(foo.versionsKey(), bytes);
        final Versions versions = this.repository.versions(foo).toCompletableFuture().join();
        final Key.From bar = new Key.From("bar");
        versions.save(this.asto, bar).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Saved versions are not identical to versions initial content",
            this.storage.value(bar),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldGetEmptyPackageVersionsWhenNonePresent() throws Exception {
        final PackageKeys pack = new PackageKeys("MyLib");
        final Versions versions = this.repository.versions(pack).toCompletableFuture().join();
        final Key.From sink = new Key.From("sink");
        versions.save(this.asto, sink).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Versions created from scratch expected to be empty",
            this.versions(sink),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void shouldFailToAddPackageWhenItAlreadyExists() throws Exception {
        this.repository.add(new Content.From(this.nupkg().bytes())).toCompletableFuture().join();
        final Throwable cause = Assertions.assertThrows(
            CompletionException.class,
            () -> this.repository.add(new Content.From(this.nupkg().bytes()))
                .toCompletableFuture().join()
        ).getCause();
        MatcherAssert.assertThat(
            cause,
            new IsInstanceOf(PackageVersionAlreadyExistsException.class)
        );
    }

    @Test
    void shouldReadNuspec() throws Exception {
        final PackageIdentity identity = new PackageIdentity(
            new PackageId("UsefulLib"),
            new Version("2.0")
        );
        this.storage.save(
            identity.nuspecKey(),
            String.join(
                "",
                "<?xml version=\"1.0\"?>",
                "<package xmlns=\"http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd\">",
                "<metadata><id>UsefulLib</id></metadata>",
                "</package>"
            ).getBytes()
        );
        MatcherAssert.assertThat(
            this.repository.nuspec(identity).toCompletableFuture().join().id().normalized(),
            new IsEqual<>("usefullib")
        );
    }

    @Test
    void shouldFailToReadNuspecWhenValueAbsent() {
        final PackageIdentity identity = new PackageIdentity(
            new PackageId("MyPack"),
            new Version("1.0")
        );
        final Throwable cause = Assertions.assertThrows(
            CompletionException.class,
            () -> this.repository.nuspec(identity).toCompletableFuture().join()
        ).getCause();
        MatcherAssert.assertThat(
            cause,
            new IsInstanceOf(IllegalArgumentException.class)
        );
    }

    @RepeatedTest(10)
    void throwsExceptionWhenPackagesAddedSimultaneously() throws Exception {
        final int count = 3;
        final CountDownLatch latch = new CountDownLatch(count);
        final List<CompletableFuture<Void>> tasks = new ArrayList<>(count);
        for (int number = 0; number < count; number += 1) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add(future);
            new Thread(
                () -> {
                    try {
                        latch.countDown();
                        latch.await();
                        this.repository.add(new Content.From(this.nupkg().bytes()))
                            .toCompletableFuture().join();
                        future.complete(null);
                    } catch (final Exception exception) {
                        future.completeExceptionally(exception);
                    }
                }
            ).start();
        }
        final List<Throwable> failures = tasks.stream().flatMap(
            task -> {
                Stream<Throwable> result;
                try {
                    task.join();
                    result = Stream.empty();
                } catch (final RuntimeException ex) {
                    result = Stream.of(ex.getCause());
                }
                return result;
            }
        ).collect(Collectors.toList());
        MatcherAssert.assertThat(
            "Some updates failed",
            failures,
            new IsNot<>(new IsEmptyCollection<>())
        );
        MatcherAssert.assertThat(
            "All failure due to concurrent lock access or that version already exists",
            failures,
            new Every<>(
                new AnyOf<>(
                    Arrays.asList(
                        new AllOf<>(
                            Arrays.asList(
                                new IsInstanceOf(ArtipieIOException.class),
                                new FeatureMatcher<Throwable, String>(
                                    new StringContains("Failed to acquire lock."),
                                    "an exception with message",
                                    "message"
                                ) {
                                    @Override
                                    protected String featureValueOf(final Throwable actual) {
                                        return actual.getMessage();
                                    }
                                }
                            )
                        ),
                        new IsInstanceOf(PackageVersionAlreadyExistsException.class)
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Storage has no locks",
            this.storage.list(Key.ROOT).stream().noneMatch(key -> key.string().contains("lock")),
            new IsEqual<>(true)
        );
    }

    private List<String> versions(final Key key) throws Exception {
        final byte[] bytes = this.storage.value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            return reader.readObject()
                .getJsonArray("versions")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(Collectors.toList());
        }
    }

    private NewtonJsonResource nupkg() {
        return new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg");
    }
}
