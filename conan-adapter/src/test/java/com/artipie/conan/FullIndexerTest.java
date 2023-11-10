/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for FullIndexer class.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseVarargs"})
class FullIndexerTest {

    /**
     * Path to zlib package binary index file.
     */
    private static final Key ZLIB_BIN_INDEX = new Key.From(
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/revisions.txt"
    );

    /**
     * Path to zlib package recipe index file.
     */
    private static final Key ZLIB_SRC_INDEX = new Key.From(
        "zlib/1.2.11/_/_/revisions.txt"
    );

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

    /**
     * Path prefix for conan repository test data.
     */
    private static final String DIR_PREFIX = "conan-test/server_data/data";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test instance.
     */
    private FullIndexer indexer;

    @BeforeEach
    public void setUp() {
        this.storage = new InMemoryStorage();
        final RevisionsIndexer revi = new RevisionsIndexer(this.storage);
        this.indexer = new FullIndexer(this.storage, revi);
    }

    @Test
    public void fullIndexEmpty() {
        this.indexer.fullIndexUpdate(new Key.From("zlib/1.2.11/_/_/")).toCompletableFuture().join();
        final boolean srcexist = this.storage.exists(FullIndexerTest.ZLIB_SRC_INDEX).join();
        final boolean binexist = this.storage.exists(FullIndexerTest.ZLIB_BIN_INDEX).join();
        MatcherAssert.assertThat(
            "Must be recipe index", srcexist
        );
        MatcherAssert.assertThat(
            "Must be no binary index", !binexist
        );
        final JsonObject obj = Json.createReader(
            new StringReader(
                new String(new BlockingStorage(this.storage).value(FullIndexerTest.ZLIB_SRC_INDEX))
            )
        ).readObject();
        final JsonArray revs = obj.getJsonArray("revisions");
        MatcherAssert.assertThat(
            "Must be no recipe revisions", revs.size() == 0
        );
    }

    @Test
    public void fullIndexUpdate() {
        for (final String file : FullIndexerTest.CONAN_TEST_PKG) {
            new TestResource(String.join("/", FullIndexerTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        this.indexer.fullIndexUpdate(new Key.From("zlib/1.2.11/_/_/")).toCompletableFuture()
            .join();
        final boolean srcexist = this.storage.exists(FullIndexerTest.ZLIB_SRC_INDEX).join();
        final boolean binexist = this.storage.exists(FullIndexerTest.ZLIB_BIN_INDEX).join();
        MatcherAssert.assertThat(
            "Must be recipe index", srcexist
        );
        MatcherAssert.assertThat(
            "Must be binary index", binexist
        );
        final JsonObject srcindex = Json.createReader(
            new StringReader(
                new String(new BlockingStorage(this.storage).value(FullIndexerTest.ZLIB_SRC_INDEX))
            )
        ).readObject();
        final JsonArray srcrevs = srcindex.getJsonArray("revisions");
        MatcherAssert.assertThat(
            "Must exist one recipe revision", srcrevs.size() == 1
        );
        final JsonObject binindex = Json.createReader(
            new StringReader(
                new String(new BlockingStorage(this.storage).value(FullIndexerTest.ZLIB_BIN_INDEX))
            )
        ).readObject();
        final JsonArray binrevs = binindex.getJsonArray("revisions");
        MatcherAssert.assertThat(
            "Must exist one binary revision", binrevs.size() == 1
        );
    }
}
