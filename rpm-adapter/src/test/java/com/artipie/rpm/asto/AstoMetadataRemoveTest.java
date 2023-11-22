/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.meta.PackageInfo;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoMetadataRemove}.
 * @since 1.9
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoMetadataRemoveTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig conf;

    /**
     * Reader of metadata bytes.
     */
    private MetadataBytes mbytes;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.mbytes = new MetadataBytes(this.storage);
        this.conf = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, true);
    }

    @Test
    void doesNothingIfStorageIsEmpty() {
        new AstoMetadataRemove(this.storage, this.conf).perform(new ListOf<String>("abc123"))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void removesPackageAndSavesChecksum() throws IOException {
        final String path = "AstoMetadataRemoveTest/removesPackageAndSavesChecksum";
        new TestResource(String.join("/", path, "primary.xml.gz"))
            .saveTo(this.storage, new Key.From("repodata", "primary.xml.gz"));
        new TestResource(String.join("/", path, "other.xml.gz"))
            .saveTo(this.storage, new Key.From("repodata", "other.xml.gz"));
        new TestResource(String.join("/", path, "filelists.xml.gz"))
            .saveTo(this.storage, new Key.From("repodata", "filelists.xml.gz"));
        final Collection<PackageInfo> infos = new ArrayList<>(1);
        final Key res = new AstoMetadataRemove(this.storage, this.conf, Optional.of(infos)).perform(
            new ListOf<String>("7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44")
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Storage has 9 items",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(9)
        );
        MatcherAssert.assertThat(
            "Failed to update primary.xml correctly",
            new TestResource(String.join("/", path, "primary.xml")).asPath(),
            new IsXmlEqual(
                this.mbytes.value(res, XmlPackage.PRIMARY)
            )
        );
        MatcherAssert.assertThat(
            "Failed to update other.xml correctly",
            new TestResource(String.join("/", path, "other.xml")).asPath(),
            new IsXmlEqual(
                this.mbytes.value(res, XmlPackage.OTHER)
            )
        );
        MatcherAssert.assertThat(
            "Failed to update filelists.xml correctly",
            new TestResource(String.join("/", path, "filelists.xml")).asPath(),
            new IsXmlEqual(
                this.mbytes.value(res, XmlPackage.FILELISTS)
            )
        );
        MatcherAssert.assertThat(
            "Removed package info was not added to list",
            infos, Matchers.contains(new PackageInfo("aom", "aarch64", "1.0.0"))
        );
        this.checksumCheck(res, XmlPackage.PRIMARY);
        this.checksumCheck(res, XmlPackage.OTHER);
        this.checksumCheck(res, XmlPackage.FILELISTS);
    }

    @Test
    void savesTheSameContentIfPackageNotFound() throws IOException {
        final String path = "AstoMetadataRemoveTest/savesTheSameContentIfPackageNotFound";
        new TestResource(String.join("/", path, "primary.xml.gz"))
            .saveTo(this.storage, new Key.From("repodata", "primary.xml.gz"));
        new TestResource(String.join("/", path, "other.xml.gz"))
            .saveTo(this.storage, new Key.From("repodata", "other.xml.gz"));
        final Key res = new AstoMetadataRemove(this.storage, this.conf)
            .perform(new ListOf<String>("abc123")).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Storage has 6 items",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(6)
        );
        MatcherAssert.assertThat(
            "Primary metadata should be not changed",
            new TestResource(String.join("/", path, "primary.xml")).asPath(),
            new IsXmlEqual(
                this.mbytes.value(res, XmlPackage.PRIMARY)
            )
        );
        MatcherAssert.assertThat(
            "Other metadata should be not changed",
            new TestResource(String.join("/", path, "other.xml")).asPath(),
            new IsXmlEqual(
                this.mbytes.value(res, XmlPackage.OTHER)
            )
        );
        this.checksumCheck(res, XmlPackage.PRIMARY);
        this.checksumCheck(res, XmlPackage.OTHER);
    }

    private void checksumCheck(final Key res, final XmlPackage other) {
        MatcherAssert.assertThat(
            String.format("Checksum and size are expected to be stored for %s", other.name()),
            new String(
                new BlockingStorage(this.storage)
                    .value(
                        new Key.From(
                            res, String.format("%s.%s", other.name(), this.conf.digest().name())
                        )
                    ),
                StandardCharsets.UTF_8
            ),
            Matchers.matchesPattern("[0-9a-z]* \\d+")
        );
    }

}
