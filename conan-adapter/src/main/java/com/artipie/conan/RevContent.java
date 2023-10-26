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
