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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link AddUpdateUserSlice}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AddUpdateUserSliceTest {
    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsBadRequestOnInvalidRequest(final RqMethod rqmeth) {
        MatcherAssert.assertThat(
            new AddUpdateUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(rqmeth, "/some/api/david")
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsNotFoundIfCredentialsAreEmpty(final RqMethod rqmeth) {
        MatcherAssert.assertThat(
            new AddUpdateUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(rqmeth, "/api/security/users/empty")
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasAddedToCredentials(final RqMethod rqmeth) throws IOException {
        final String username = "mike";
        final String pswd = "qwerty123";
        final Storage storage = new InMemoryStorage();
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        final Key key = new Key.From("_credentials.yaml");
        this.creds("person", storage, key);
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(
                new Settings.Fake(new Credentials.FromStorageYaml(storage, key))
            ).response(rqline.toString(), Headers.EMPTY, this.jsonBody(pswd)),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            Yaml.createYamlInput(
                new PublisherAs(storage.value(key).join())
                    .asciiString().toCompletableFuture().join()
            ).readYamlMapping().value("credentials")
                .asMapping().value(username)
                .asMapping().string("pass"),
            new IsEqual<>(this.plainPswd(pswd))
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasUpdated(final RqMethod rqmeth) throws IOException {
        final String username = "mike";
        final String newpswd = "qwerty123";
        final Storage storage = new InMemoryStorage();
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        final Key key = new Key.From("_credentials.yaml");
        this.creds(username, storage, key);
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(
                new Settings.Fake(new Credentials.FromStorageYaml(storage, key))
            ).response(rqline.toString(), Headers.EMPTY, this.jsonBody(newpswd)),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "User with updated password should return",
            Yaml.createYamlInput(
                new PublisherAs(storage.value(key).join())
                    .asciiString().toCompletableFuture().join()
            ).readYamlMapping().value("credentials")
                .asMapping().value(username)
                .asMapping().string("pass"),
            new IsEqual<>(this.plainPswd(newpswd))
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

    private Flowable<ByteBuffer> jsonBody(final String pswd) {
        return Flowable.fromArray(
            ByteBuffer.wrap(
                Json.createObjectBuilder()
                    .add("password", pswd)
                    .build().toString().getBytes()
            )
        );
    }

    private String plainPswd(final String pswd) {
        return String.format("plain:%s", pswd);
    }
}
