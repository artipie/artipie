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
import com.artipie.api.ContentAs;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeleteUserSlice}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DeleteUserSliceTest {
    @Test
    void returnsNotFoundOnInvalidRequest() {
        MatcherAssert.assertThat(
            new DeleteUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/some/api/david")
            )
        );
    }

    @Test
    void returnsNotFoundIfCredentialsAreEmpty() {
        MatcherAssert.assertThat(
            new DeleteUserSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/users/empty")
            )
        );
    }

    @Test
    void returnsNotFoundIfUserIsNotFoundInCredentials() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("_credentials.yaml");
        this.creds("john", storage, key);
        MatcherAssert.assertThat(
            new DeleteUserSlice(new Settings.Fake(new Credentials.FromStorageYaml(storage, key))),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/users/notfound")
            )
        );
    }

    @Test
    void returnsOkAndDeleteIfUserIsFoundInCredentials() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("_credentials.yaml");
        this.creds("jane", storage, key);
        MatcherAssert.assertThat(
            "DeleteUserSlice response",
            new DeleteUserSlice(new Settings.Fake(new Credentials.FromStorageYaml(storage, key))),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.DELETE, "/api/security/users/jane")
            )
        );
        MatcherAssert.assertThat(
            "User should be deleted from storage",
            storage.value(key)
                .thenApply(cred -> this.yaml(storage, key))
                .thenCompose(yaml -> yaml)
                .toCompletableFuture().join()
                .string("credentials"),
            new IsNull<>()
        );
    }

    /**
     * Credentials as yaml.
     * @param storage Storage
     * @param key Key for storage
     * @return Completion action with yaml
     */
    private CompletionStage<YamlMapping> yaml(final Storage storage, final Key key) {
        return new RxStorageWrapper(storage)
            .value(key)
            .to(ContentAs.YAML)
            .to(SingleInterop.get())
            .thenApply(yaml -> (YamlMapping) yaml);
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
