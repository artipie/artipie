/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link AstoCreateRepomd}.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoCreateRepomdTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Test config.
     */
    private RepoConfig conf;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.conf = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA256, true);
    }

    @Test
    void createsFileWhenStorageIsEmpty() {
        new AstoCreateRepomd(this.asto, this.conf).perform(Key.ROOT)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(this.asto).value(new Key.From("repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']"
            )
        );
    }

    @Test
    void createsRepomd() {
        final Charset charset = StandardCharsets.UTF_8;
        final BlockingStorage blsto = new BlockingStorage(this.asto);
        final Key temp = new Key.From("temp");
        blsto.save(
            new Key.From(temp, XmlPackage.PRIMARY.name()), "primary.xml.gz".getBytes(charset)
        );
        blsto.save(new Key.From(temp, XmlPackage.OTHER.name()), "other.xml.gz".getBytes(charset));
        blsto.save(
            new Key.From(temp, XmlPackage.FILELISTS.name()), "filelists.xml.gz".getBytes(charset)
        );
        new MapOf<XmlPackage, String>(
            new MapEntry<>(XmlPackage.PRIMARY, "o_primary_checksum 123"),
            new MapEntry<>(XmlPackage.OTHER, "o_other_checksum 34"),
            new MapEntry<>(XmlPackage.FILELISTS, "o_filelists_checksum 76")
        ).forEach(
            (key, val) -> blsto.save(
                new Key.From(temp, key.name(), this.conf.digest().name()),
                val.getBytes(charset)
            )
        );
        new AstoCreateRepomd(this.asto, this.conf).perform(temp).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new TestResource("AstoCreateRepomdTest/repomd-res.xml").asBytes(),
            CompareMatcher.isSimilarTo(
                blsto.value(new Key.From(temp, "repomd.xml"))
            )
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(node -> "revision".equals(node.getLocalName()))
        );
    }

}
