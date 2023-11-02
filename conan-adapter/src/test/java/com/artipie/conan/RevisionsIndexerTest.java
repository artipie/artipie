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
import java.time.Instant;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for RevisionsIndexApi class.
 * @since 0.1
 */
@SuppressWarnings({"PMD.UseVarargs", "PMD.AvoidDuplicateLiterals"})
class RevisionsIndexerTest {

    /**
     * Path prefix for conan repository test data.
     */
    private static final String DIR_PREFIX = "conan-test/server_data/data/";

    /**
     * Package recipe (sources) subdir name.
     */
    private static final String SRC_SUBDIR = "export";

    /**
     * Path to zlib package.
     */
    private static final String ZLIB_SRC_PKG = "zlib/1.2.11/_/_";

    /**
     * Path to zlib package recipe index file.
     */
    private static final String ZLIB_SRC_INDEX = "zlib/1.2.11/_/_/revisions.txt";

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
     * Test storage.
     */
    private Storage storage;

    /**
     * Test instance.
     */
    private RevisionsIndexer indexer;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.indexer = new RevisionsIndexer(this.storage);
    }

    @Test
    void emptyStorage() {
        final Key pkgkey = new Key.From(RevisionsIndexerTest.ZLIB_SRC_PKG);
        final List<Integer> result = this.indexer.buildIndex(
            pkgkey, PackageList.PKG_SRC_LIST, (name, rev) -> new Key.From(
                pkgkey.string(), rev.toString(), RevisionsIndexerTest.SRC_SUBDIR, name
            )).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "The revisions list isn't empty for empty storage",
            result.isEmpty()
        );
    }

    @Test
    void indexBuild() {
        final Key pkgkey = new Key.From(RevisionsIndexerTest.ZLIB_SRC_PKG);
        for (final String file : RevisionsIndexerTest.CONAN_TEST_PKG) {
            new TestResource(String.join("", RevisionsIndexerTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final List<Integer> result = this.indexer.buildIndex(
            pkgkey, PackageList.PKG_SRC_LIST, (name, rev) -> new Key.From(
                pkgkey.string(), rev.toString(), RevisionsIndexerTest.SRC_SUBDIR, name
            )).toCompletableFuture().join();
        final Key key = new Key.From(RevisionsIndexerTest.ZLIB_SRC_INDEX);
        final JsonParser parser = Json.createParser(
            new StringReader(new String(new BlockingStorage(this.storage).value(key)))
        );
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray("revisions");
        final String time = RevisionsIndexerTest.getJsonStr(revs.get(0), "time");
        final String revision = RevisionsIndexerTest.getJsonStr(revs.get(0), "revision");
        MatcherAssert.assertThat(
            "The revision object fields have incorrect format",
            time.length() > 0 && revision.length() > 0 && result.size() == revs.size()
        );
        MatcherAssert.assertThat(
            "The revision field of revision object has incorrect value",
            result.get(0) == Integer.parseInt(revision)
        );
        MatcherAssert.assertThat(
            "The time field of the revision object has incorrect value",
            Instant.parse(time).getEpochSecond() > 0
        );
    }

    private static String getJsonStr(final JsonValue object, final String key) {
        return object.asJsonObject().get(key).toString().replaceAll("\"", "");
    }
}
