/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.index;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.http.NuGet;
import com.artipie.security.policy.Policy;
import io.reactivex.Flowable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

/**
 * Tests for {@link NuGet}.
 * Service index resource.
 */
class NuGetServiceIndexTest {

    /**
     * Base URL for services.
     */
    private URL url;

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    @BeforeEach
    void init() throws Exception {
        this.url = URI.create("http://localhost:4321/repo").toURL();
        this.nuget = new NuGet(
            this.url,
            new AstoRepository(new InMemoryStorage()),
            Policy.FREE, (username, password) -> Optional.empty(), "*", Optional.empty()
        );
    }

    @Test
    void shouldGetIndex() {
        final Response response = this.nuget.response(
            new RequestLine(RqMethod.GET, "/index.json"),
            Headers.EMPTY,
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        new IsJson(
                            new AllOf<>(
                                Arrays.asList(
                                    new JsonHas("version", new JsonValueIs("3.0.0")),
                                    new JsonHas(
                                        "resources",
                                        new JsonContains(
                                            new IsService(
                                                "PackagePublish/2.0.0",
                                                String.format("%s/package", this.url)
                                            ),
                                            new IsService(
                                                "RegistrationsBaseUrl/Versioned",
                                                String.format("%s/registrations", this.url)
                                            ),
                                            new IsService(
                                                "PackageBaseAddress/3.0.0",
                                                String.format("%s/content", this.url)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    void shouldFailPutIndex() {
        final Response response = this.nuget.response(
            new RequestLine(RqMethod.PUT, "/index.json"),
            Headers.EMPTY,
            Flowable.empty()
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED));
    }

    /**
     * Matcher for bytes array representing JSON.
     *
     * @since 0.1
     */
    private class IsJson extends TypeSafeMatcher<byte[]> {

        /**
         * Matcher for JSON.
         */
        private final Matcher<? extends JsonObject> json;

        IsJson(final Matcher<? extends JsonObject> json) {
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
     * Matcher for JSON object representing service.
     *
     * @since 0.1
     */
    private class IsService extends BaseMatcher<JsonObject> {

        /**
         * Expected service type.
         */
        private final String type;

        /**
         * Expected service id.
         */
        private final String id;

        IsService(final String type, final String id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public void describeTo(final Description description) {
            this.delegate().describeTo(description);
        }

        @Override
        public boolean matches(final Object item) {
            return this.delegate().matches(item);
        }

        private Matcher<JsonObject> delegate() {
            return new AllOf<>(
                Arrays.asList(
                    new JsonHas("@type", new JsonValueIs(this.type)),
                    new JsonHas("@id", new JsonValueIs(this.id))
                )
            );
        }
    }
}
