/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.text.IsEmptyString;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Matcher for errors response.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class IsErrorsResponse extends BaseMatcher<Response> {

    /**
     * Delegate matcher.
     */
    private final Matcher<Response> delegate;

    /**
     * Ctor.
     *
     * @param status Expected response status code.
     * @param code Expected error code.
     */
    public IsErrorsResponse(final RsStatus status, final String code) {
        this.delegate = new AllOf<>(
            Arrays.asList(
                new RsHasStatus(status),
                new RsHasHeaders(
                    Matchers.containsInRelativeOrder(
                        new Header("Content-Type", "application/json; charset=utf-8")
                    )
                ),
                new RsHasBody(
                    new IsJson(
                        new JsonHas(
                            "errors",
                            new JsonContains(new IsError(code))
                        )
                    )
                )
            )
        );
    }

    @Override
    public boolean matches(final Object actual) {
        return this.delegate.matches(actual);
    }

    @Override
    public void describeTo(final Description description) {
        this.delegate.describeTo(description);
    }

    /**
     * Matcher for bytes array representing JSON.
     *
     * @since 0.5
     */
    private static class IsJson extends TypeSafeMatcher<byte[]> {

        /**
         * Matcher for JSON.
         */
        private final Matcher<? extends JsonValue> json;

        /**
         * Ctor.
         *
         * @param json Matcher for parsed JSON.
         */
        IsJson(final Matcher<? extends JsonValue> json) {
            this.json = json;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("JSON ").appendDescriptionOf(this.json);
        }

        @Override
        public boolean matchesSafely(final byte[] bytes) {
            final JsonObject root;
            try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                root = reader.readObject();
            }
            return this.json.matches(root);
        }
    }

    /**
     * Matcher for JSON object representing error.
     *
     * @since 0.5
     */
    private static class IsError extends BaseMatcher<JsonObject> {

        /**
         * Expected code.
         */
        private final Matcher<JsonObject> delegate;

        /**
         * Ctor.
         *
         * @param code Expected error code.
         */
        IsError(final String code) {
            this.delegate = new AllOf<>(
                Arrays.asList(
                    new JsonHas("code", new JsonValueIs(code)),
                    new JsonHas(
                        "message",
                        new JsonValueIs(new IsNot<>(IsEmptyString.emptyString()))
                    )
                )
            );
        }

        @Override
        public void describeTo(final Description description) {
            this.delegate.describeTo(description);
        }

        @Override
        public boolean matches(final Object item) {
            return this.delegate.matches(item);
        }
    }
}
