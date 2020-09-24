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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.Settings;
import com.artipie.Users;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

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
    void returnsBadRequestIfCredentialsAreEmpty(final RqMethod rqmeth) {
        MatcherAssert.assertThat(
            new AddUpdateUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(rqmeth, "/api/security/users/empty"),
                Headers.EMPTY,
                new Content.From(
                    Json.createObjectBuilder().build()
                    .toString().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasAddedToCredentials(final RqMethod rqmeth) throws IOException {
        final String username = "mark";
        final String pswd = "abc123";
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        final List<String> groups = new ListOf<>("readers", "a-team");
        this.creds("person", Collections.emptyList());
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(
                new Settings.Fake(
                    new Users.FromStorageYaml(this.storage, new Key.From("_credentials.yaml"))
                )
            ).response(
                rqline.toString(), Headers.EMPTY,
                this.jsonBody(pswd, username, groups)
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            this.readCreds(username).string("pass"),
            new IsEqual<>(this.shaPswd(pswd))
        );
        MatcherAssert.assertThat(
            "User has groups",
            this.readCreds(username).yamlSequence("groups")
                .values().stream().map(node -> node.asScalar().value())
                .collect(Collectors.toList()),
            new IsEqual<>(groups)
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasUpdated(final RqMethod rqmeth) throws IOException {
        final String username = "mike";
        final String newpswd = "qwerty123";
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        this.creds(username, Collections.emptyList());
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(
                new Settings.Fake(
                    new Users.FromStorageYaml(this.storage, new Key.From("_credentials.yaml"))
                )
            ).response(
                rqline.toString(), Headers.EMPTY,
                this.jsonBody(newpswd, username, Collections.emptyList())
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "User with updated password should return",
            this.readCreds(username).string("pass"),
            new IsEqual<>(this.shaPswd(newpswd))
        );
        MatcherAssert.assertThat(
            "Yaml has no groups",
            this.readCreds(username).yamlSequence("groups"),
            new IsNull<>()
        );
    }

    private void creds(final String username, final List<String> groups) {
        YamlMappingBuilder user = Yaml.createYamlMappingBuilder().add("pass", "pain:123");
        if (!groups.isEmpty()) {
            YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
            for (final String group : groups) {
                seq = seq.add(group);
            }
            user = user.add("groups", seq.build());
        }
        this.storage.save(
            new Key.From("_credentials.yaml"),
            new Content.From(Yaml.createYamlMappingBuilder()
                .add(
                    "credentials",
                    Yaml.createYamlMappingBuilder().add(
                        username,
                        user.build()
                    ).build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    private Flowable<ByteBuffer> jsonBody(final String pswd, final String name,
        final List<String> groups) {
        final JsonObjectBuilder json = Json.createObjectBuilder()
            .add("password", pswd)
            .add("email", String.format("%s@example.com", name));
        if (!groups.isEmpty()) {
            json.add("groups", Json.createArrayBuilder(groups).build());
        }
        return Flowable.fromArray(ByteBuffer.wrap(json.build().toString().getBytes()));
    }

    private String shaPswd(final String pswd) {
        return String.format("sha256:%s", DigestUtils.sha256Hex(pswd));
    }

    private YamlMapping readCreds(final String username) throws IOException {
        return Yaml.createYamlInput(
            new PublisherAs(this.storage.value(new Key.From("_credentials.yaml")).join())
                .asciiString().toCompletableFuture().join()
        ).readYamlMapping().yamlMapping("credentials").yamlMapping(username);
    }
}
