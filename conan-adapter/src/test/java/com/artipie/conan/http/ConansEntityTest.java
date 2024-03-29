/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.json.Json;
import java.util.function.Function;

/**
 * Test for {@link ConansEntity}.
 */
class ConansEntityTest {

    /**
     * Path prefix for conan repository test data.
     */
    private static final String DIR_PREFIX = "conan-test/server_data/data";

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
    public void downloadBinTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/packages/dfbe50feef7f3c6223a476cd5aeadb687084a646/download_urls",
            "http/download_bin_urls.json", ConansEntityTest.CONAN_TEST_PKG, ConansEntity.DownloadBin::new
        );
    }

    @Test
    public void downloadSrcTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/download_urls", "http/download_src_urls.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.DownloadSrc::new
        );
    }

    @Test
    public void getSearchBinPkgTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/search", "http/pkg_bin_search.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.GetSearchBinPkg::new
        );
    }

    @Test
    public void getPkgInfoTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/packages/dfbe50feef7f3c6223a476cd5aeadb687084a646",
            "http/pkg_bin_info.json", ConansEntityTest.CONAN_TEST_PKG, ConansEntity.GetPkgInfo::new
        );
    }

    @Test
    public void getSearchSrcPkgTest() throws JSONException {
        this.runTest(
            "/v1/conans/search?q=zlib", "http/pkg_src_search.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.GetSearchSrcPkg::new
        );
    }

    @Test
    public void digestForPkgSrcTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/digest", "http/pkg_digest.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.DigestForPkgSrc::new
        );
    }

    @Test
    public void digestForPkgBinTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_/packages/dfbe50feef7f3c6223a476cd5aeadb687084a646/digest", "http/pkg_digest_bin.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.DigestForPkgBin::new
        );
    }

    @Test
    void getSrcPkgInfoTest() throws JSONException {
        this.runTest(
            "/v1/conans/zlib/1.2.11/_/_", "http/pkg_src_info.json",
            ConansEntityTest.CONAN_TEST_PKG, ConansEntity.GetSrcPkgInfo::new
        );
    }

    /**
     * Runs test on given set of files and request factory. Checks the match with json given.
     * JSONAssert is used for friendly json matching error messages.
     * @param request HTTP request string.
     * @param json Path to json file with expected response value.
     * @param files List of files required for test.
     * @param factory Request instance factory.
     * @throws JSONException For Json parsing errors.
     */
    private void runTest(final String request, final String json, final String[] files,
        final Function<Storage, Slice> factory) throws JSONException {
        final Storage storage = new InMemoryStorage();
        for (final String file : files) {
            new TestResource(String.join("/", ConansEntityTest.DIR_PREFIX, file))
                .saveTo(storage, new Key.From(file));
        }
        final ResponseImpl response = factory.apply(storage).response(
            new RequestLine(RqMethod.GET, request),
            Headers.from("Host", "localhost:9300"), Content.EMPTY
        ).join();
        final String expected = Json.createReader(
            new TestResource(json).asInputStream()
        ).readObject().toString();
        JSONAssert.assertEquals(expected, response.body().asString(), true);
    }
}
