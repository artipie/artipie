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
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetUserSlice}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class GetUserSliceTest {

    @Test
    void returnsNotFoundOnInvalidRequest() {
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/some/api/david")
            )
        );
    }

    @Test
    void returnsNotFoundIfUserIsNotFoundInCredentials() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("_credentials.yaml");
        this.creds("john", storage, key);
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake(new Credentials.FromStorageYaml(storage, key))),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/security/users/josh")
            )
        );
    }

    @Test
    void returnsJsonFoundIfUserFound() {
        final String username = "jerry";
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("_cred.yaml");
        this.creds(username, storage, key);
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake(new Credentials.FromStorageYaml(storage, key))),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        Json.createObjectBuilder()
                            .add("name", username)
                            .add(
                                "email",
                                String.format("%s@artipie.com", username)
                            )
                            .add("lastLoggedIn", "2020-01-01T01:01:01.000+01:00")
                            .add("realm", "Internal")
                            .build().toString(),
                        StandardCharsets.UTF_8
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/users/%s", username))
            )
        );
    }

    private void creds(final String username, final Storage storage, final Key key) {
        storage.save(
            key,
                new Content.From(Yaml.createYamlMappingBuilder()
                .add(
                    "credentials",
                    Yaml.createYamlMappingBuilder().add(
                        username,
                        Yaml.createYamlMappingBuilder().add("pass", "pain:123").build()
                    ).build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

}
