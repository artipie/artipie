/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.management.Users;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import wtf.g4s8.tuples.Pair;

/**
 * Test for {@link UsersFromStorageYaml}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"unchecked", "PMD.AvoidDuplicateLiterals"})
class UsersFromStorageYamlTest {

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
    void readsYamlWithEmailFromStorage() {
        final Users.User jane = new Users.User(
            "jane", this.email("jane"), new SetOf<String>("readers")
        );
        final Users.User john = new Users.User(
            "john", this.email("john"), new SetOf<String>("reviewers", "supporters")
        );
        final Users.PasswordFormat sha = Users.PasswordFormat.SHA256;
        final String pass = "111";
        this.creds(sha, Pair.of(jane, pass), Pair.of(john, pass));
        MatcherAssert.assertThat(
            new UsersFromStorageYaml(this.storage, this.key).list()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder(jane, john)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ""})
    void readsYamlFromStorage(final String extension) {
        final Users.User jane = new Users.User("maria");
        final Users.User john = new Users.User("olga");
        new CredsConfigYaml().withUsers(jane.name(), john.name())
            .saveTo(this.storage, this.key);
        MatcherAssert.assertThat(
            new UsersFromStorageYaml(
                this.storage, new Key.From(String.format("_cred%s", extension))
            ).list()
            .toCompletableFuture().join(),
            Matchers.containsInAnyOrder(jane, john)
        );
    }

    @Test
    void addsUser() {
        final Users.User maria = new Users.User(
            "maria", this.email("maria"), new SetOf<>("newbies", "tester")
        );
        final Users.User olga = new Users.User(
            "olga", this.email("olga"), new SetOf<>("readers", "a-team")
        );
        final String pass = "abc";
        final Users.PasswordFormat sha = Users.PasswordFormat.SHA256;
        this.creds(sha, Pair.of(maria, pass));
        new UsersFromStorageYaml(this.storage, this.key)
            .add(olga, DigestUtils.sha256Hex(pass), sha).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYamlWithEmailAndGroups(
                    sha,
                    Pair.of(maria, pass),
                    Pair.of(olga, pass)
                )
            )
        );
    }

    @Test
    void updatesUser() {
        final Users.User jack = new Users.User("jack");
        final Users.User silvia = new Users.User(
            "silvia", this.email("silvia"), new SetOf<>("readers")
        );
        final String old = "345";
        final String newpass = "000";
        final Users.PasswordFormat plain = Users.PasswordFormat.PLAIN;
        this.creds(plain, Pair.of(jack, old), Pair.of(silvia, old));
        new UsersFromStorageYaml(this.storage, this.key)
            .add(silvia, newpass, Users.PasswordFormat.PLAIN).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYamlWithEmailAndGroups(
                    plain,
                    Pair.of(jack, old),
                    Pair.of(silvia, newpass)
                )
            )
        );
    }

    @Test
    void removesUser() {
        final Users.User mark = new Users.User("mark");
        final Users.User ann = new Users.User("ann");
        final String pass = "123";
        final Users.PasswordFormat plain = Users.PasswordFormat.PLAIN;
        this.creds(plain, Pair.of(mark, pass), Pair.of(ann, pass));
        new UsersFromStorageYaml(this.storage, this.key).remove(ann.name())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(this.getYamlWithEmailAndGroups(plain, Pair.of(mark, pass)))
        );
    }

    @Test
    void doNotChangeYamlOnRemoveIfUserNotFound() {
        final Users.User ted = new Users.User("ted");
        final Users.User alex = new Users.User("alex");
        final String pass = "098";
        final Users.PasswordFormat plain = Users.PasswordFormat.PLAIN;
        this.creds(plain, Pair.of(ted, pass), Pair.of(alex, pass));
        new UsersFromStorageYaml(this.storage, this.key).remove("alice")
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.storage.value(this.key).join())
                .asciiString().toCompletableFuture().join(),
            new IsEqual<>(
                this.getYamlWithEmailAndGroups(
                    plain,
                    Pair.of(ted, pass), Pair.of(alex, pass)
                )
            )
        );
    }

    private void creds(final Users.PasswordFormat format, final Pair<Users.User, String>... users) {
        this.storage.save(
            this.key,
            new Content.From(
                this.getYamlWithEmailAndGroups(format, users).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    private String getYamlWithEmailAndGroups(final Users.PasswordFormat format,
        final Pair<Users.User, String>... users) {
        final CredsConfigYaml creds = new CredsConfigYaml();
        for (final Pair<Users.User, String> pair : users) {
            pair.accept(
                (user, pass) -> creds.withFullInfo(
                    user.name(), format, pass,
                    this.email(user.name()).get(),
                    user.groups()
                )
            );
        }
        return creds.toString();
    }

    private Optional<String> email(final String name) {
        return Optional.of(String.format("%s@example.com", name));
    }

}
