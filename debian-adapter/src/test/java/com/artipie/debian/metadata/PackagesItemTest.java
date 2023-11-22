/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PackagesItem.Asto}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PackagesItemTest {

    @Test
    void addsInfoAndSorts() {
        final Storage asto = new InMemoryStorage();
        final Key.From key = new Key.From("some/debian/package.deb");
        asto.save(key, new Content.From("abc123".getBytes())).join();
        MatcherAssert.assertThat(
            new PackagesItem.Asto(
                asto
            ).format(
                String.join(
                    "\n",
                    "Version: 1.7-3",
                    "Package: aglfn",
                    "Architecture: all",
                    "Maintainer: Debian Fonts Task Force <pkg-fonts-devel@lists.alioth.debian.org>",
                    "Installed-Size: 138",
                    "Section: fonts"
                ),
                key
            ).toCompletableFuture().join(),
            new IsEqual<>(
                String.join(
                    "\n",
                    "Package: aglfn",
                    "Version: 1.7-3",
                    "Architecture: all",
                    "Maintainer: Debian Fonts Task Force <pkg-fonts-devel@lists.alioth.debian.org>",
                    "Installed-Size: 138",
                    "Section: fonts",
                    "Filename: some/debian/package.deb",
                    "Size: 6",
                    "MD5sum: e99a18c428cb38d5f260853678922e03",
                    "SHA1: 6367c48dd193d56ea7b0baad25b19455e529f5ee",
                    "SHA256: 6ca13d52ca70c883e0f0bb101e425a89e8624de51db2d2392593af6a84118090"
                )
            )
        );
    }

}
