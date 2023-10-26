/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
