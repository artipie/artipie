/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.http.RpmUpload;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoRepoAdd}.
 * @since 1.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoRepoAddTest {

    /**
     * Metadata key.
     */
    private static final Key MTD = new Key.From("repodata");

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Reader of metadata bytes.
     */
    private MetadataBytes mbytes;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.mbytes = new MetadataBytes(this.storage);
    }

    @Test
    void createsEmptyMetadata() throws IOException {
        new AstoRepoAdd(
            this.storage,
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, false)
        ).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to generate 3 items: primary, filelists and repomd",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(3)
        );
        MatcherAssert.assertThat(
            "Failed to generate empty primary xml",
            new String(
                this.mbytes.value(XmlPackage.PRIMARY),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='metadata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            "Failed to generate empty other xml",
            new String(
                this.mbytes.value(XmlPackage.OTHER),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='otherdata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(AstoRepoAddTest.MTD, "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='repomd']/*[local-name()='revision']")
        );
    }

    @Test
    void addsPackagesToRepo() throws IOException {
        new TestResource("AstoRepoAddTest/filelists.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "filelists.xml.gz"));
        new TestResource("AstoRepoAddTest/other.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "other.xml.gz"));
        new TestResource("AstoRepoAddTest/primary.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "primary.xml.gz"));
        final String time = "time-1.7-45.el7.x86_64.rpm";
        new TestResource(time).saveTo(this.storage, new Key.From(RpmUpload.TO_ADD, time));
        final String lib = "libnss-mymachines2-245-1.x86_64.rpm";
        new TestResource(lib).saveTo(this.storage, new Key.From(RpmUpload.TO_ADD, "lib", lib));
        this.storage.save(
            new Key.From(RpmUpload.TO_ADD, "invalid.rpm"),
            new Content.From(new TestRpm.Invalid().bytes())
        ).join();
        new AstoRepoAdd(
            this.storage,
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, true)
        ).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to have 6 items in storage: primary, other, filelists, repomd and 2 rpms",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(6)
        );
        MatcherAssert.assertThat(
            "Failed to add `time` rpm to the correct location",
            this.storage.exists(new Key.From(time)).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Failed to add `lib` rpm to the correct location",
            this.storage.exists(new Key.From("lib", lib)).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Primary xml should have three records",
            new String(this.mbytes.value(XmlPackage.PRIMARY), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='3']",
                //@checkstyle LineLengthCheck (3 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libnss-mymachines2']"
            )
        );
        MatcherAssert.assertThat(
            "Other xml should have three records",
            new String(
                this.mbytes.value(XmlPackage.OTHER),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='3']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='time']",
                //@checkstyle LineLengthCheck (1 line)
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='libnss-mymachines2']"
            )
        );
        MatcherAssert.assertThat(
            "Filelists xml should have three records",
            new String(
                this.mbytes.value(XmlPackage.FILELISTS),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='filelists' and @packages='3']",
                "/*[local-name()='filelists']/*[local-name()='package' and @name='abc']",
                "/*[local-name()='filelists']/*[local-name()='package' and @name='time']",
                //@checkstyle LineLengthCheck (1 line)
                "/*[local-name()='filelists']/*[local-name()='package' and @name='libnss-mymachines2']"
            )
        );
        MatcherAssert.assertThat(
            "Failed to generate repomd xml",
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(AstoRepoAddTest.MTD, "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='primary']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='other']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='filelists']"
            )
        );
    }

    @Test
    void doesNothingIfOnlyInvalidPackageIsInUpdate() throws IOException {
        new TestResource("AstoRepoAddTest/other.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "other.xml.gz"));
        new TestResource("AstoRepoAddTest/primary.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "primary.xml.gz"));
        this.storage.save(
            new Key.From(RpmUpload.TO_ADD, "invalid.rpm"),
            new Content.From(new TestRpm.Invalid().bytes())
        ).join();
        new AstoRepoAdd(
            this.storage,
            new RepoConfig.Simple(
                Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA256), false
            )
        ).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to have 3 items in storage: primary, other, and repomd",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(3)
        );
        this.checkMeta("primary.xml", XmlPackage.PRIMARY);
        this.checkMeta("other.xml", XmlPackage.OTHER);
        MatcherAssert.assertThat(
            "Failed to generate repomd xml",
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(AstoRepoAddTest.MTD, "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='primary']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='other']"
            )
        );
    }

    private void checkMeta(final String file, final XmlPackage primary) throws IOException {
        MatcherAssert.assertThat(
            String.format("Failed to generate %s xml", primary.lowercase()),
            new TestResource(String.format("AstoRepoAddTest/%s", file)).asPath(),
            new IsXmlEqual(
                this.mbytes.value(primary)
            )
        );
    }
}
