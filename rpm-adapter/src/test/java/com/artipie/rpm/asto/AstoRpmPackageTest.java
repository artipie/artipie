/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redline_rpm.header.Header;

/**
 * Test for {@link AstoRpmPackage}.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class AstoRpmPackageTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void readsPackageMeta() throws IOException {
        final String name = "time-1.7-45.el7.x86_64.rpm";
        new TestResource(name).saveTo(this.storage);
        final Package.Meta meta = new AstoRpmPackage(this.storage, Digest.SHA256)
            .packageMeta(new Key.From(name)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to calc checksum",
            meta.checksum().hex(),
            new IsEqual<>("fdb381e12e4fa1d4e4b7680b2ca90813b5048c42a0a41d7f1270b5a5d3a5358f")
        );
        MatcherAssert.assertThat(
            "Failed to calc size",
            meta.size(),
            new IsEqual<>(31_064L)
        );
        MatcherAssert.assertThat(
            "Failed to read header",
            meta.header(Header.HeaderTag.NAME).asStrings(),
            Matchers.contains("time")
        );
    }

}
