/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.metadata;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.PackageIdentity;
import com.artipie.nuget.PackageKeys;
import com.artipie.nuget.Versions;
import com.artipie.nuget.http.NuGet;
import com.artipie.nuget.http.TestAuthentication;
import com.artipie.nuget.metadata.Nuspec;
import com.artipie.nuget.metadata.Version;
import com.artipie.security.policy.PolicyByUsername;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/**
 * Tests for {@link NuGet}.
 * Package metadata resource.
 */
class NuGetPackageMetadataTest {

    private NuGet nuget;

    private InMemoryStorage storage;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        this.nuget = new NuGet(
            URI.create("http://localhost:4321/repo").toURL(),
            new AstoRepository(this.storage),
            new PolicyByUsername(TestAuthentication.USERNAME),
            new TestAuthentication(),
            "test",
            Optional.empty()
        );
    }

    @Test
    void shouldGetRegistration() {
        new Versions()
            .add(new Version("12.0.3"))
            .save(
                this.storage,
                new PackageKeys("Newtonsoft.Json").versionsKey()
            );
        final Nuspec.Xml nuspec = new Nuspec.Xml(
            String.join(
                "",
                "<?xml version=\"1.0\"?>",
                "<package xmlns=\"http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd\">",
                "<metadata><id>Newtonsoft.Json</id><version>12.0.3</version></metadata>",
                "</package>"
            ).getBytes()
        );
        this.storage.save(
            new PackageIdentity(nuspec.id(), nuspec.version()).nuspecKey(),
            new Content.From(nuspec.bytes())
        ).join();
        final Response response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/registrations/newtonsoft.json/index.json"
            ),
            TestAuthentication.HEADERS,
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(new IsValidRegistration())
                )
            )
        );
    }

    @Test
    void shouldGetRegistrationsWhenEmpty() {
        final Response response = this.nuget.response(
            new RequestLine(
                RqMethod.GET,
                "/registrations/my.lib/index.json"
            ),
            TestAuthentication.HEADERS,
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(new IsValidRegistration())
                )
            )
        );
    }

    @Test
    void shouldFailPutRegistration() {
        final Response response = this.nuget.response(
            new RequestLine(
                RqMethod.PUT,
                "/registrations/newtonsoft.json/index.json"
            ),
            TestAuthentication.HEADERS,
            Content.EMPTY
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED));
    }

    @Test
    void shouldUnauthorizedGetRegistrationForAnonymousUser() {
        MatcherAssert.assertThat(
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/registrations/my-utils/index.json"
                ),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new ResponseMatcher(
                RsStatus.UNAUTHORIZED, Headers.EMPTY
            )
        );
    }

    /**
     * Matcher for bytes array representing valid Registration JSON.
     *
     * @since 0.1
     */
    private static class IsValidRegistration extends TypeSafeMatcher<byte[]> {

        @Override
        public void describeTo(final Description description) {
            description.appendText("is registration JSON");
        }

        @Override
        public boolean matchesSafely(final byte[] bytes) {
            final JsonObject root;
            try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                root = reader.readObject();
            }
            return root.getInt("count") == root.getJsonArray("items").size();
        }
    }
}
