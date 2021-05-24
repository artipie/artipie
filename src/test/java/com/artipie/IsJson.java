/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
