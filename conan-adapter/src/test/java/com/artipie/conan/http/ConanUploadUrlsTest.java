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
package  com.artipie.conan.http;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.conan.ItemTokenizer;
import com.artipie.conan.ItemTokenizer.ItemInfo;
import com.artipie.http.Response;
import com.artipie.http.hm.IsJson;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import io.vertx.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import org.cactoos.map.MapEntry;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;

/**
 * Test for {@link ConanUpload}.
 * @since 0.1
 * @checkstyle LineLengthCheck (999 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (999 lines)
 */
public class ConanUploadUrlsTest {

    @Test
    void tokenizerTest() {
        final String path = "/test/path/to/file";
        final String host = "test_hostname.com";
        final ItemTokenizer tokenizer = new ItemTokenizer(Vertx.vertx());
        final String token = tokenizer.generateToken(path, host);
        final ItemInfo item = tokenizer.authenticateToken(token).toCompletableFuture().join().get();
        MatcherAssert.assertThat("Decoded path must match", item.getPath().equals(path));
        MatcherAssert.assertThat("Decoded host must match", item.getHostname().equals(host));
    }

    @Test
    void uploadsUrlsKeyByPath() throws Exception {
        final Storage storage = new InMemoryStorage();
        final String payload =
            "{\"conan_export.tgz\": \"\", \"conanfile.py\":\"\", \"conanmanifest.txt\": \"\"}";
        final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        final Response response = new ConanUpload.UploadUrls(storage, new ItemTokenizer(Vertx.vertx())).response(
            new RequestLine(
                "POST",
                "/v1/conans/zmqpp/4.2.0/_/_/upload_urls",
                "HTTP/1.1"
            ).toString(),
            Arrays.asList(
                new MapEntry<>("Content-Size", Long.toString(data.length)),
                new MapEntry<>("Host", "localhost")
            ),
            Flowable.just(ByteBuffer.wrap(data))
        );
        MatcherAssert.assertThat(
            "Response body must match",
            response,
            Matchers.allOf(
                new RsHasStatus(RsStatus.OK),
                new RsHasBody(
                    new IsJson(
                        Matchers.allOf(
                            new JsonHas(
                                "conan_export.tgz",
                                new JsonValueStarts("http://localhost/zmqpp/4.2.0/_/_/0/export/conan_export.tgz?signature=")
                            ),
                            new JsonHas(
                                "conanfile.py",
                                new JsonValueStarts(
                                    "http://localhost/zmqpp/4.2.0/_/_/0/export/conanfile.py?signature="
                                )
                            ),
                            new JsonHas(
                                "conanmanifest.txt",
                                new JsonValueStarts(
                                    "http://localhost/zmqpp/4.2.0/_/_/0/export/conanmanifest.txt?signature="
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    /**
     * Checks that json string value start with the prefix provided.
     * @since 0.1
     */
    private static class JsonValueStarts extends TypeSafeMatcher<JsonValue> {

        /**
         * Prefix string value for matching.
         */
        private final String prefix;

        /**
         * Creates json prefix matcher with provided prefix.
         * @param prefix Prefix string value.
         */
        JsonValueStarts(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void describeTo(final Description desc) {
            desc.appendText("prefix: ")
                .appendValue(this.prefix)
                .appendText(" of type ")
                .appendValue(ValueType.STRING);
        }

        @Override
        protected boolean matchesSafely(final JsonValue item) {
            boolean matches = false;
            if (item.getValueType().equals(ValueType.STRING) && item.toString().startsWith(this.prefix, 1)) {
                matches = true;
            }
            return matches;
        }
    }
}
