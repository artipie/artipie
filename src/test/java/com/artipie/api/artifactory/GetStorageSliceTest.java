/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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
package com.artipie.api.artifactory;

import com.artipie.IsJson;
import com.artipie.api.ArtipieApi;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.file.Path;
import java.util.Arrays;
import javax.json.JsonValue;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test for {@link ArtipieApi}.
 *
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@Disabled
class GetStorageSliceTest {

    /**
     * Temporary directory to use as storage.
     *
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    @ParameterizedTest
    @CsvSource({
        "flat,my-lib",
        "org,my-company/my-lib"
    })
    void shouldReturnExpectedData(final String layout, final String repo) {
        MatcherAssert.assertThat(
            new GetStorageSlice(this.example(this.tmp, repo)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(
                            new IsJson(
                                new JsonHas(
                                    "files",
                                    new JsonContains(
                                        this.entryMatcher("/foo/bar/1", "false"),
                                        this.entryMatcher("/foo/bar/baz", "true")
                                    )
                                )
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/storage/%s/foo/bar", repo))
            )
        );
    }

    private Matcher<? extends JsonValue> entryMatcher(final String uri, final String folder) {
        return new AllOf<>(
            Arrays.asList(
                new JsonHas("uri", new JsonValueIs(uri)),
                new JsonHas("folder", new JsonValueIs(folder))
            )
        );
    }

    private Storage example(final Path temp, final String name) {
        final Storage storage = new LoggingStorage(new InMemoryStorage());
        storage.save(
            new Key.From(String.format("%s.yaml", name)),
            new Content.From(
                String.join(
                    "\n",
                    "repo:",
                    "  type: file",
                    "  storage:",
                    "    type: fs",
                    String.format("    path: %s", temp.toString())
                ).getBytes()
            )
        );
        final Storage repo = new SubStorage(new Key.From(name), new FileStorage(temp));
        repo.save(new Key.From("foo/bar/1"), Content.EMPTY).join();
        repo.save(new Key.From("foo/bar/baz/2"), Content.EMPTY).join();
        repo.save(new Key.From("foo/3"), Content.EMPTY).join();
        return storage;
    }
}
