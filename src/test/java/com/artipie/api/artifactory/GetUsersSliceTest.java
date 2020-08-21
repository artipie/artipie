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
package com.artipie.api.artifactory;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.Credentials;
import com.artipie.Settings;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetUsersSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class GetUsersSliceTest {

    /**
     * Artipie base url.
     */
    private static final String BASE = "http://artipie.com/";

    /**
     * Artipie yaml meta section.
     */
    private static final YamlMapping META = Yaml.createYamlMappingBuilder()
        .add("base_url", GetUsersSliceTest.BASE).build();

    @Test
    void returnsNotFoundIfNoCredentialsFound() {
        MatcherAssert.assertThat(
            new GetUsersSlice(
                new Settings.Fake(Optional.empty(), GetUsersSliceTest.META)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    @Test
    void returnsUsersList() throws IOException {
        final String jane = "jane";
        final String john = "john";
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("_cred.yaml");
        this.creds(storage, key, jane, john);
        MatcherAssert.assertThat(
            new GetUsersSlice(
                new Settings.Fake(
                    Optional.of(new Credentials.FromStorageYaml(storage, key)),
                    GetUsersSliceTest.META
                )
            ),
            new SliceHasResponse(
                new RsHasBody(
                    Json.createArrayBuilder()
                        .add(this.getUserJson(jane))
                        .add(this.getUserJson(john))
                        .build().toString().getBytes(StandardCharsets.UTF_8)
                ),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    private JsonObject getUserJson(final String user) {
        return Json.createObjectBuilder()
            .add("name", user)
            .add("uri", String.format("%sapi/security/users/%s", GetUsersSliceTest.BASE, user))
            .add("realm", "Internal")
            .build();
    }

    private void creds(final Storage storage, final Key key,
        final String... users) throws IOException {
        storage.save(
            key,
            new Content.From(
                Yaml.createYamlInput(
                    String.join(
                        "\n",
                        "credentials:",
                        Stream.of(users).map(
                            user -> String.format("  %s:\n%s", user, "    pass: plain123")
                        ).collect(Collectors.joining("\n"))
                    )
                ).readYamlMapping().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }
}
