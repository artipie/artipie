/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Content;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArray;

/**
 * Class represents revision content for Conan package.
 * @since 0.1
 */
public class RevContent {

    /**
     * Revisions json field.
     */
    private static final String REVISIONS = "revisions";

    /**
     * Revision content.
     */
    private final JsonArray content;

    /**
     * Initializes new instance.
     * @param content Array of revisions.
     */
    public RevContent(final JsonArray content) {
        this.content = content;
    }

    /**
     * Creates revisions content object for array of revisions.
     * @return Artipie Content object with revisions data.
     */
    public Content toContent() {
        return new Content.From(Json.createObjectBuilder()
            .add(RevContent.REVISIONS, this.content)
            .build().toString().getBytes(StandardCharsets.UTF_8)
        );
    }
}
