/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.asto.MetadataBytes;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.hm.StorageHasMetadata;
import com.artipie.rpm.hm.StorageHasRepoMd;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.list.Mapped;
import org.cactoos.scalar.AndInThreads;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.llorllale.cactoos.matchers.IsTrue;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 * @todo #110:30min Meaningful error on broken package.
 *  Rpm should throw an exception when trying to add an invalid package.
 *  The type of exception must be IllegalArgumentException and its message
 *  "Reading of RPM package 'package' failed, data corrupt or malformed.",
 *  like described in showMeaningfulErrorWhenInvalidPackageSent. Implement it
 *  and then enable the test.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
final class RpmTest {

    /**
     * Temporary directory for all tests.
         */
    @TempDir
    static Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig config;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.config = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA1, true);
    }

    @Test
    void updatesDifferentReposSimultaneouslyTwice() throws Exception {
        final Rpm repo =  new Rpm(this.storage, this.config);
        final List<String> keys = new ListOf<>("one", "two", "three");
        final CountDownLatch latch = new CountDownLatch(keys.size());
        final List<Scalar<Boolean>> tasks = new Mapped<>(
            key -> new Unchecked<>(
                () -> {
                    new TestRpm.Multiple(
                        new TestRpm.Abc(),
                        new TestRpm.Libdeflt()
                    ).put(new SubStorage(new Key.From(key), this.storage));
                    latch.countDown();
                    latch.await();
                    repo.batchUpdate(new Key.From(key)).blockingAwait();
                    return true;
                }
            ),
            keys
        );
        new AndInThreads(tasks).value();
        new AndInThreads(tasks).value();
        keys.forEach(
            key -> MatcherAssert.assertThat(
                new SubStorage(new Key.From(key), this.storage),
                Matchers.allOf(
                    new StorageHasRepoMd(this.config),
                    new StorageHasMetadata(2, this.config.filelists(), RpmTest.tmp)
                )
            )
        );
    }

    @Test
    void updateWorksOnNewRepo() throws IOException {
        new TestRpm.Multiple(
            new TestRpm.Abc(), new TestRpm.Libdeflt(), new TestRpm.Time()
        ).put(this.storage);
        new Rpm(this.storage, this.config).batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            Matchers.allOf(
                new StorageHasMetadata(3, this.config.filelists(), RpmTest.tmp),
                new StorageHasRepoMd(this.config)
            )
        );
    }

    @Test
    void doesNotTouchMetadataIfInvalidRpmIsSent() throws Exception {
        final RepoConfig cnfg =
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, true);
        final Rpm repo = new Rpm(this.storage, cnfg);
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Libdeflt()).put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final Storage stash = new InMemoryStorage();
        new Copy(
            this.storage,
            this.storage.list(new Key.From("repodata")).join().stream()
                .filter(item -> item.string().endsWith("gz")).collect(Collectors.toList())
        ).copy(stash).join();
        new TestRpm.Invalid().put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        for (final Key key : stash.list(Key.ROOT).join()) {
            MatcherAssert.assertThat(
                String.format("%s xmls are equal", key.string()),
                new MetadataBytes(this.storage).value(key),
                CompareMatcher.isSimilarTo(new MetadataBytes(stash).value(key))
                    .ignoreWhitespace().normalizeWhitespace()
            );
        }
    }

    @Test
    void skipsInvalidPackageOnUpdate() throws Exception {
        final Rpm repo =  new Rpm(this.storage, this.config);
        new TestRpm.Abc().put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Multiple(new TestRpm.Invalid(), new TestRpm.Libdeflt()).put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            Matchers.allOf(
                new StorageHasMetadata(2, true, RpmTest.tmp),
                new StorageHasRepoMd(this.config)
            )
        );
    }

    @Test
    @Disabled
    void showMeaningfulErrorWhenInvalidPackageSent() throws Exception {
        final Rpm repo = new Rpm(
            this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Invalid().put(this.storage);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> repo.batchUpdate(Key.ROOT).blockingAwait(),
            "Reading of RPM package \"brokentwo.rpm\" failed, data corrupt or malformed."
        );
    }

    @Test
    void throwsExceptionWhenFullUpdatesDoneSimultaneously() throws IOException {
        final Rpm repo =  new Rpm(
            this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        final List<Key> keys = Collections.nCopies(3, Key.ROOT);
        final CountDownLatch latch = new CountDownLatch(keys.size());
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(this.storage);
        final List<CompletableFuture<Void>> tasks = new ArrayList<>(keys.size());
        for (final Key key : keys) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add(future);
            new Thread(
                () -> {
                    try {
                        latch.countDown();
                        latch.await();
                        repo.batchUpdate(key).blockingAwait();
                        future.complete(null);
                    } catch (final Exception exception) {
                        future.completeExceptionally(exception);
                    }
                }
            ).start();
        }
        for (final CompletableFuture<Void> task : tasks) {
            try {
                task.join();
            } catch (final Exception ignored) {
            }
        }
        MatcherAssert.assertThat(
            "Some updates failed",
            tasks.stream().anyMatch(CompletableFuture::isCompletedExceptionally),
            new IsTrue()
        );
        MatcherAssert.assertThat(
            "Storage has no locks",
            this.storage.list(Key.ROOT).join().stream()
                .noneMatch(key -> key.string().contains("lock")),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "''",
        "my_repo",
        "one/two/three",
        "a/b/"
    })
    void writesSubdirsToLocation(final String str) throws IOException {
        final Rpm repo =  new Rpm(this.storage, StandardNamingPolicy.PLAIN, Digest.SHA256, true);
        final Key key = new Key.From(str);
        final Storage substorage = new SubStorage(key, this.storage);
        new TestRpm.Abc().put(new SubStorage(new Key.From("subdir"), substorage));
        new TestRpm.Libdeflt().put(substorage);
        repo.batchUpdate(key).blockingAwait();
        final Path gzip = Files.createTempFile(RpmTest.tmp, XmlPackage.PRIMARY.name(), "xml.gz");
        Files.write(
            gzip, new BlockingStorage(substorage).value(new Key.From("repodata/primary.xml.gz"))
        );
        final Path xml = Files.createTempFile(RpmTest.tmp, XmlPackage.PRIMARY.name(), "xml");
        new Gzip(gzip).unpack(xml);
        MatcherAssert.assertThat(
            new XMLDocument(xml),
            XhtmlMatchers.hasXPath(
                //@checkstyle LineLengthCheck (3 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='location' and @href='libdeflt1_0-2020.03.27-25.1.armv7hl.rpm']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='location' and @href='subdir/abc-1.01-26.git20200127.fc32.ppc64le.rpm']"
            )
        );
    }

    @Test
    void removesPackagesFromMetadata() throws Exception {
        final Rpm repo =  new Rpm(this.storage, this.config);
        new TestRpm.Abc().put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Libdeflt().put(this.storage);
        this.storage.delete(new Key.From(new TestRpm.Abc().path().getFileName().toString())).join();
        repo.batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            Matchers.allOf(
                new StorageHasMetadata(1, true, RpmTest.tmp),
                new StorageHasRepoMd(this.config)
            )
        );
    }

}
