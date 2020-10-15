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
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.CredsConfigYaml;
import com.artipie.Settings;
import com.artipie.Users;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ApiChangeUserPassword}.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ApiChangeUserPasswordTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Credentials key.
     */
    private Key key;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.key = new Key.From("_credentials.yaml");
    }

    @Test
    void returnsFoundIfUserWasAddedToCredentials() throws IOException {
        final String username = "mike";
        final String pswd = "qwerty123";
        new CredsConfigYaml().withUsers("person").saveTo(this.storage, this.key);
        MatcherAssert.assertThat(
            "ApiChangeUserPassword response should be FOUND",
            new ApiChangeUserPassword(
                new Settings.Fake(new Users.FromStorageYaml(this.storage, this.key))
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.FOUND),
                    new RsHasHeaders(
                        new Headers.From("Location", String.format("/dashboard/%s", username))
                    )
                ),
                new RequestLine(RqMethod.PUT, String.format("/api/users/%s/password", username)),
                Headers.EMPTY,
                this.body(pswd)
            )
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            this.getPasswordFromYaml(username),
            new IsEqual<>(this.pswd(pswd))
        );
    }

    @Test
    void returnsFoundIfPasswordWasUpdated() throws IOException {
        final String username = "john";
        final String pswd = "0000";
        new CredsConfigYaml().withUsers(username).saveTo(this.storage, this.key);
        MatcherAssert.assertThat(
            "ApiChangeUserPassword response should be FOUND",
            new ApiChangeUserPassword(
                new Settings.Fake(new Users.FromStorageYaml(this.storage, this.key))
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.FOUND),
                    new RsHasHeaders(
                        new Headers.From("Location", String.format("/dashboard/%s", username))
                    )
                ),
                new RequestLine(RqMethod.PUT, String.format("/api/users/%s/password", username)),
                Headers.EMPTY,
                this.body(pswd)
            )
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            this.getPasswordFromYaml(username),
            new IsEqual<>(this.pswd(pswd))
        );
    }

    private String getPasswordFromYaml(final String username) throws IOException {
        return Yaml.createYamlInput(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join()
        ).readYamlMapping().value("credentials")
            .asMapping().value(username)
            .asMapping().string("pass");
    }

    private Content body(final String pswd) {
        return new Content.From(String.format("password=%s", pswd).getBytes());
    }

    private String pswd(final String pswd) {
        return String.format("sha256:%s", DigestUtils.sha256Hex(pswd));
    }

}
