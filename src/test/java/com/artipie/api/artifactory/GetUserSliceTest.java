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
import com.artipie.Settings;
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
    void returnsNotFoundIfCredentialsAreEmpty() {
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/security/users/mark")
            )
        );
    }

    @Test
    void returnsNotFoundIfUserIsNotFoundInCredentials() {
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake(this.creds("john"))),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/security/users/josh")
            )
        );
    }

    @Test
    void returnsJsonFoundIfUserFound() {
        final String username = "jerry";
        MatcherAssert.assertThat(
            new GetUserSlice(new Settings.Fake(this.creds(username))),
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

    private YamlMapping creds(final String username) {
        return Yaml.createYamlMappingBuilder()
            .add(
                "credentials",
                Yaml.createYamlMappingBuilder().add(
                    username,
                    Yaml.createYamlMappingBuilder().add("pass", "pain:123").build()
                ).build()
            ).build();
    }

}
