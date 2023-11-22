/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for PackageList class.
 * @since 0.1
 */
@SuppressWarnings("PMD.UseVarargs")
class PackageListTest {

    /**
     * Path to zlib package.
     */
    private static final String ZLIB_SRC_PKG = "zlib/1.2.11/_/_";

    /**
     * Path prefix for conan repository test data.
     */
    private static final String DIR_PREFIX = "conan-test/server_data/data/";

    /**
     * Conan server zlib package files list for unit tests.
     */
    private static final String[] CONAN_TEST_PKG = {
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conaninfo.txt",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conan_package.tgz",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conanmanifest.txt",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/revisions.txt",
        "zlib/1.2.11/_/_/0/export/conan_export.tgz",
        "zlib/1.2.11/_/_/0/export/conanfile.py",
        "zlib/1.2.11/_/_/0/export/conanmanifest.txt",
        "zlib/1.2.11/_/_/0/export/conan_sources.tgz",
        "zlib/1.2.11/_/_/revisions.txt",
    };

    @Test
    public void emptyList() {
        final Key pkgkey = new Key.From(PackageListTest.ZLIB_SRC_PKG);
        final PackageList list = new PackageList(new InMemoryStorage());
        final List<String> bins = list.get(new Key.From(pkgkey, "exports"))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "List must be empty for empty storage",
            bins.isEmpty()
        );
    }

    @Test
    public void packageList() {
        final Key pkgkey = new Key.From(PackageListTest.ZLIB_SRC_PKG);
        final Storage storage = new InMemoryStorage();
        for (final String file : PackageListTest.CONAN_TEST_PKG) {
            new TestResource(String.join("", PackageListTest.DIR_PREFIX, file))
                .saveTo(storage, new Key.From(file));
        }
        final PackageList list = new PackageList(storage);
        final List<String> bins = list.get(new Key.From(pkgkey, "0/package"))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Invalid package list size",
            bins.size() == 1
        );
        MatcherAssert.assertThat(
            "Invalid binary package id",
            bins.get(0).equals("dfbe50feef7f3c6223a476cd5aeadb687084a646")
        );
    }
}
