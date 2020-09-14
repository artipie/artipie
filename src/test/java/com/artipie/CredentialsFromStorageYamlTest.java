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
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Credentials.FromStorageYaml}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("unchecked")
class CredentialsFromStorageYamlTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test key.
     */
    private Key key;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.key = new Key.From("_cred.yaml");
    }

    @Test
    @Disabled
    void readsYamlFromStorage() {
        final String jane = "jane";
        final String john = "john";
        final String pass = "sha256:111";
        this.creds(new ImmutablePair<>(jane, pass), new ImmutablePair<>(john, pass));
        MatcherAssert.assertThat(
            new Credentials.FromStorageYaml(this.storage, this.key).users()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder(
                new Credentials.User(jane, this.email(jane)),
                new Credentials.User(john, this.email(john))
            )
        );
    }

    @Test
    void addsUser() {
        final String maria = "maria";
        final String olga = "olga";
        final String pass = "abc";
        final String full = String.format("sha256:%s", pass);
        this.creds(new ImmutablePair<>(maria, full));
        new Credentials.FromStorageYaml(this.storage, this.key).add(
            new Credentials.User(olga, this.email(olga)), pass, Credentials.PasswordFormat.SHA256
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYaml(
                    new ImmutablePair<>(maria, full),
                    new ImmutablePair<>(olga, full)
                )
            )
        );
    }

    @Test
    void updatesUser() {
        final String jack = "jack";
        final String silvia = "silvia";
        final String old = "plain:345";
        final String newpass = "000";
        this.creds(new ImmutablePair<>(jack, old), new ImmutablePair<>(silvia, old));
        new Credentials.FromStorageYaml(this.storage, this.key)
            .add(
                new Credentials.User(silvia, this.email(silvia)),
                newpass, Credentials.PasswordFormat.PLAIN
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYaml(
                    new ImmutablePair<>(jack, old),
                    new ImmutablePair<>(silvia, String.format("plain:%s", newpass))
                )
            )
        );
    }

    @Test
    void removesUser() {
        final String mark = "mark";
        final String ann = "ann";
        final String pass = "plain:123";
        this.creds(new ImmutablePair<>(mark, pass), new ImmutablePair<>(ann, pass));
        new Credentials.FromStorageYaml(this.storage, this.key).remove(ann)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(this.getYaml(new ImmutablePair<>(mark, pass)))
        );
    }

    @Test
    void doNotChangeYamlOnRemoveIfUserNotFound() {
        final String ted = "ted";
        final String alex = "alex";
        final String pass = "plain:098";
        this.creds(new ImmutablePair<>(ted, pass), new ImmutablePair<>(alex, pass));
        new Credentials.FromStorageYaml(this.storage, this.key).remove("alice")
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYaml(new ImmutablePair<>(ted, pass), new ImmutablePair<>(alex, pass))
            )
        );
    }

    private void creds(final Pair<String, String>... users) {
        this.storage.save(
            this.key,
            new Content.From(this.getYaml(users).getBytes(StandardCharsets.UTF_8))
        ).join();
    }

    private String getYaml(final Pair<String, String>... users) {
        YamlMappingBuilder mapping = Yaml.createYamlMappingBuilder();
        for (final Pair<String, String> user : users) {
            mapping = mapping.add(
                user.getKey(),
                Yaml.createYamlMappingBuilder()
                    .add("pass", user.getValue())
                    .add("email", this.email(user.getKey()).get())
                    .build()
            );
        }
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            mapping.build()
        ).build().toString();
    }

    private Optional<String> email(final String name) {
        return Optional.of(String.format("%s@example.com", name));
    }

}
