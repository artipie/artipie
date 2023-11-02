/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.files.TestBundle;
import com.artipie.rpm.hm.StorageHasMetadata;
import com.artipie.rpm.hm.StorageHasRepoMd;
import com.artipie.rpm.misc.UncheckedConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link Rpm}.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledIfSystemProperty(named = "it.longtests.enabled", matches = "true")
@ExtendWith(TimingExtension.class)
final class RpmITCase {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    static Path tmp;

    /**
     * Gzipped bundle of RPMs.
     */
    private static Path bundle;

    /**
     * Test bundle size.
     */
    private static final TestBundle.Size SIZE =
        TestBundle.Size.valueOf(
            System.getProperty("it.longtests.size", "hundred")
                .toUpperCase(Locale.US)
        );

    /**
     * Repository storage with RPM packages.
     */
    private Storage storage;

    @BeforeAll
    static void setUpClass() throws Exception {
        RpmITCase.bundle = new TestBundle(RpmITCase.SIZE).load(RpmITCase.tmp);
    }

    @BeforeEach
    void setUp() throws Exception {
        final Path repo = Files.createDirectory(RpmITCase.tmp.resolve("repo"));
        new Gzip(RpmITCase.bundle).unpackTar(repo);
        this.storage = new FileStorage(repo.resolve(RpmITCase.SIZE.filename()));
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteDirectory(RpmITCase.tmp.resolve("repo").toFile());
    }

    @Test
    void generatesMetadata() throws IOException {
        final int size = this.modifyRepo();
        final boolean filelist = true;
        new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, filelist)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            new StorageHasMetadata(size, filelist, RpmITCase.tmp)
        );
    }

    @Test
    void dontKeepOldMetadata() {
        new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        MatcherAssert.assertThat(
            "got 4 metadata files after first update",
            bsto.list(new Key.From("repodata")).size(),
            Matchers.equalTo(4)
        );
        for (int cnt = 0; cnt < 5; ++cnt) {
            final Key first = bsto.list(Key.ROOT).stream()
                .filter(name -> name.string().endsWith(".rpm"))
                .findFirst().orElseThrow(() -> new IllegalStateException("not key found"));
            bsto.delete(first);
            new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
                .batchUpdate(Key.ROOT)
                .blockingAwait();
        }
        MatcherAssert.assertThat(
            "got 4 metadata files after second update",
            bsto.list(new Key.From("repodata")).size(),
            Matchers.equalTo(4)
        );
    }

    @Test
    void generatesRepomdMetadata() throws IOException, InterruptedException {
        this.modifyRepo();
        final RepoConfig config =
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA1, true);
        new Rpm(this.storage, config)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            new StorageHasRepoMd(config)
        );
    }

    /**
     * Modifies repo by removing/adding several rpms and returns count of the rpm packages in
     * the repository after modification.
     * @return Rpm packages count after modification
     * @throws IOException On error
     */
    private int modifyRepo() throws IOException {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.list(Key.ROOT).stream()
            .filter(name -> name.string().contains("oxygen"))
            .forEach(new UncheckedConsumer<>(item -> bsto.delete(new Key.From(item))));
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Libdeflt()).put(this.storage);
        return (int) bsto.list(Key.ROOT).stream()
            .filter(item -> item.string().endsWith(".rpm")).count();
    }
}
