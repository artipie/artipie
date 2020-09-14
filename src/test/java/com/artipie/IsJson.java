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
package com.artipie;

import java.io.ByteArrayInputStream;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for bytes array representing JSON.
 *
 * @since 0.11
 */
public final class IsJson extends TypeSafeMatcher<byte[]> {

    /**
     * Matcher for JSON.
     */
    private final Matcher<? extends JsonValue> json;

    /**
     * Ctor.
     *
     * @param json Matcher for JSON.
     */
    public IsJson(final Matcher<? extends JsonValue> json) {
        this.json = json;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("JSON ").appendDescriptionOf(this.json);
    }

    @Override
    public boolean matchesSafely(final byte[] bytes) {
        final JsonValue root;
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            root = reader.readValue();
        }
        return this.json.matches(root);
    }
}
