/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Content;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.stream.JsonParser;
import java.io.StringReader;

/**
 * Tests for RevContent class.
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
        final JsonParser parser = Json.createParser(new StringReader(content.asString()));
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray(RevContentTest.REVISIONS);
        MatcherAssert.assertThat("The json array must be empty", revs.isEmpty());
    }

    @Test
    public void contentGeneration() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        final int testval = 1;
        builder.add(testval);
        final RevContent revc = new RevContent(builder.build());
        final Content content = revc.toContent();
        final JsonParser parser = Json.createParser(new StringReader(content.asString()));
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray(RevContentTest.REVISIONS);
        MatcherAssert.assertThat(
            "The size of the json array is incorrect",
            revs.size() == 1
        );
        MatcherAssert.assertThat(
            "The json array data has incorrect value",
            revs.get(0).toString().equals(Integer.toString(testval))
        );
    }
}
