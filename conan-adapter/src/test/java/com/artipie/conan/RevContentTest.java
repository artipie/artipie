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

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.stream.JsonParser;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for RevContent class.
 * @since 0.1
 */
class RevContentTest {

    /**
     * Revisions json field.
     */
    private static final String REVISIONS = "revisions";

    @Test
    public void emptyContent() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        final RevContent revc = new RevContent(builder.build());
        final Content content = revc.toContent();
        final JsonParser parser = new PublisherAs(content).asciiString().thenApply(
            str -> Json.createParser(new StringReader(str))
        ).toCompletableFuture().join();
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray(RevContentTest.REVISIONS);
        MatcherAssert.assertThat(
            "The json array must be empty",
            revs.size() == 0
        );
    }

    @Test
    public void contentGeneration() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        final int testval = 1;
        builder.add(testval);
        final RevContent revc = new RevContent(builder.build());
        final Content content = revc.toContent();
        final JsonParser parser = new PublisherAs(content).asciiString().thenApply(
            str -> Json.createParser(new StringReader(str))
        ).toCompletableFuture().join();
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray(RevContentTest.REVISIONS);
        MatcherAssert.assertThat(
            "The size of the json array is incorrent",
            revs.size() == 1
        );
        MatcherAssert.assertThat(
            "The json array data has incorrect value",
            revs.get(0).toString().equals(Integer.toString(testval))
        );
    }
}
