/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import java.util.Objects;
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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseVarargs"})
class RevisionsIndexApiTest {

    /**
     * ZLIB binary package dir. name (hash).
     */
    private static final String ZLIB_BIN_PKG = "dfbe50feef7f3c6223a476cd5aeadb687084a646";

    /**
     * Path to zlib package binary index file.
     */
    private static final String ZLIB_BIN_INDEX =
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/revisions.txt";

    /**
     * Path to zlib package recipe index file.
     */
    private static final String ZLIB_SRC_INDEX = "zlib/1.2.11/_/_/revisions.txt";

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

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test instance.
     */
    private RevisionsIndexApi index;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.index = new RevisionsIndexApi(this.storage, new Key.From("zlib/1.2.11/_/_"));
    }

    @Test
    void updateRecipeIndex() {
        for (final String file : RevisionsIndexApiTest.CONAN_TEST_PKG) {
            new TestResource(String.join("", RevisionsIndexApiTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final List<Integer> result = this.index.updateRecipeIndex().toCompletableFuture().join();
        final Key key = new Key.From(RevisionsIndexApiTest.ZLIB_SRC_INDEX);
        final JsonParser parser = Json.createParser(
            new StringReader(new String(new BlockingStorage(this.storage).value(key)))
        );
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray("revisions");
        final String time = RevisionsIndexApiTest.getJsonStr(revs.get(0), "time");
        final String revision = RevisionsIndexApiTest.getJsonStr(revs.get(0), "revision");
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

    @Test
    void updateBinaryIndex() {
        for (final String file : RevisionsIndexApiTest.CONAN_TEST_PKG) {
            new TestResource(String.join("", RevisionsIndexApiTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final List<Integer> result = this.index.updateBinaryIndex(
            0, RevisionsIndexApiTest.ZLIB_BIN_PKG
        ).toCompletableFuture().join();
        final Key key = new Key.From(RevisionsIndexApiTest.ZLIB_BIN_INDEX);
        final JsonParser parser = Json.createParser(
            new StringReader(new String(new BlockingStorage(this.storage).value(key)))
        );
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray("revisions");
        final String time = RevisionsIndexApiTest.getJsonStr(revs.get(0), "time");
        final String revision = RevisionsIndexApiTest.getJsonStr(revs.get(0), "revision");
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

    @Test
    void getRecipeRevisions() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        final List<Integer> revs = this.index.getRecipeRevisions().toCompletableFuture().join();
        MatcherAssert.assertThat("Expect 1 recipe revision", revs.size() == 1);
        MatcherAssert.assertThat("rev[0] must be zero", revs.get(0) == 0);
    }

    @Test
    void getBinaryRevisions() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        final List<Integer> binrevs = this.index.getBinaryRevisions(
            0, RevisionsIndexApiTest.ZLIB_BIN_PKG
        ).toCompletableFuture().join();
        MatcherAssert.assertThat("Expect 1 binary revision", binrevs.size() == 1);
        MatcherAssert.assertThat("binrev must be zero", binrevs.get(0) == 0);
    }

    @Test
    void getPackageList() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        final List<String> pkgs = this.index.getPackageList(0)
            .toCompletableFuture().join();
        MatcherAssert.assertThat("Expect 1 package binary", pkgs.size() == 1);
        MatcherAssert.assertThat(
            "Got correct package binary hash",
            Objects.equals(pkgs.get(0), RevisionsIndexApiTest.ZLIB_BIN_PKG)
        );
    }

    @Test
    void getLastRecipeRevision() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        MatcherAssert.assertThat(
            "Last (max) recipe revison must be zero",
            this.index.getLastRecipeRevision().join() == 0
        );
    }

    @Test
    void getLastBinaryRevision() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        MatcherAssert.assertThat(
            "Last (max) binary revision must be zero",
            this.index.getLastBinaryRevision(
                0, RevisionsIndexApiTest.ZLIB_BIN_PKG
            ).join() == 0
        );
    }

    @Test
    void fullIndexUpdateTest() {
        for (final String file : RevisionsIndexApiTest.CONAN_TEST_PKG) {
            new TestResource(String.join("", RevisionsIndexApiTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        this.index.fullIndexUpdate().toCompletableFuture().join();
        final List<Integer> revs = this.index.getRecipeRevisions().toCompletableFuture().join();
        final List<Integer> binrevs = this.index.getBinaryRevisions(
            0, RevisionsIndexApiTest.ZLIB_BIN_PKG
        ).toCompletableFuture().join();
        MatcherAssert.assertThat("Expect 1 recipe revision", revs.size() == 1);
        MatcherAssert.assertThat("rev[0] must be zero", revs.get(0) == 0);
        MatcherAssert.assertThat("Expect 1 binary revision", binrevs.size() == 1);
        MatcherAssert.assertThat("binrev[0] must be zero", binrevs.get(0) == 0);
    }

    @Test
    void recipeRevisionsUpdateTest() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        MatcherAssert.assertThat(
            "Last recipe revision must be zero",
            this.index.getLastRecipeRevision().join() == 0
        );
        this.index.addRecipeRevision(1).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Last recipe revision must be 1",
            this.index.getLastRecipeRevision().join() == 1
        );
        this.index.removeRecipeRevision(1).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Last recipe revision must be zero",
            this.index.getLastRecipeRevision().join() == 0
        );
    }

    @Test
    void binaryRevisionsUpdateTest() {
        new TestResource(RevisionsIndexApiTest.DIR_PREFIX).addFilesTo(this.storage, Key.ROOT);
        this.index.addBinaryRevision(0, RevisionsIndexApiTest.ZLIB_BIN_PKG, 1)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Last binary revision must be 1",
            this.index.getLastBinaryRevision(
                0, RevisionsIndexApiTest.ZLIB_BIN_PKG
            ).join() == 1
        );
        this.index.removeBinaryRevision(0, RevisionsIndexApiTest.ZLIB_BIN_PKG, 1)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Last binary revision must be zero",
            this.index.getLastBinaryRevision(
                0, RevisionsIndexApiTest.ZLIB_BIN_PKG
            ).join() == 0
        );
    }

    private static String getJsonStr(final JsonValue object, final String key) {
        return object.asJsonObject().get(key).toString().replaceAll("\"", "");
    }
}
