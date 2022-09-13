/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.CredsConfigYaml;
import com.artipie.settings.users.CrudUsers;
import com.artipie.settings.users.Users;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link ManageUsers}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManageUsersTest {

    /**
     * Test credentials key.
     */
    static final Key KEY = new Key.From("creds.yaml");

    /**
     * Test storage.
     */
    private BlockingStorage blsto;

    /**
     * Test users.
     */
    private CrudUsers users;

    @BeforeEach
    void init() {
        this.blsto = new BlockingStorage(new InMemoryStorage());
        this.users = new ManageUsers(ManageUsersTest.KEY, this.blsto);
    }

    @Test
    void listUsers() throws JSONException {
        this.blsto.save(
            ManageUsersTest.KEY,
            new CredsConfigYaml().withUserAndGroups("Alice", List.of("readers"))
                .withFullInfo(
                    "Bob", Users.PasswordFormat.PLAIN, "xyz", "bob@example.com", Set.of("admin")
                ).toString().getBytes(StandardCharsets.UTF_8)
        );
        JSONAssert.assertEquals(
            this.users.list().toString(),
            // @checkstyle LineLengthCheck (1 line)
            "[{\"name\":\"Alice\",\"groups\":[\"readers\"]},{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"groups\":[\"admin\"]}]",
            true
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsEmptyList(final boolean add) {
        if (add) {
            this.blsto.save(ManageUsersTest.KEY, new byte[]{});
        }
        MatcherAssert.assertThat(
            this.users.list(),
            new IsEmptyCollection<>()
        );
    }

    @ParameterizedTest
    @MethodSource("creds")
    void addsUser(final Pair<YamlMapping, Boolean> pair) {
        if (pair.getRight()) {
            this.blsto.save(
                ManageUsersTest.KEY,
                pair.getLeft().toString().getBytes(StandardCharsets.UTF_8)
            );
        }
        final String alice = "Alice";
        final String email = "Alice@example.com";
        this.users.addOrUpdate(
            Json.createObjectBuilder().add("type", "plain").add("pass", "xyz")
                .add("email", email)
                .add("groups", Json.createArrayBuilder().add("reader").add("creator").build())
                .build(),
            alice
        );
        final JsonArray list = this.users.list();
        MatcherAssert.assertThat(
            "Failed to find added user",
            list.stream().anyMatch(usr -> alice.equals(usr.asJsonObject().getString("name")))
        );
        MatcherAssert.assertThat(
            "Failed to add user",
            new String(this.blsto.value(ManageUsersTest.KEY), StandardCharsets.UTF_8),
            new StringContains(
                String.join(
                    System.lineSeparator(),
                    "  Alice:",
                    "    type: plain",
                    "    pass: xyz",
                    "    email: Alice@example.com",
                    "    groups:",
                    "      - reader",
                    "      - creator"
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("creds")
    void throwsErrorWhenUserNotExist(final Pair<YamlMapping, Boolean> pair) {
        if (pair.getRight()) {
            this.blsto.save(
                ManageUsersTest.KEY,
                pair.getLeft().toString().getBytes(StandardCharsets.UTF_8)
            );
        }
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.users.remove("Alice")
        );
    }

    @Test
    void removesUser() {
        this.blsto.save(
            ManageUsersTest.KEY,
            new CredsConfigYaml().withUsers("John", "Mark").yaml().toString()
                .getBytes(StandardCharsets.UTF_8)
        );
        this.users.remove("John");
        MatcherAssert.assertThat(
            this.users.list().size(),
            new IsEqual<>(1)
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Pair<YamlMapping, Boolean>> creds() {
        return Stream.of(
            new ImmutablePair<>(
                new CredsConfigYaml().withFullInfo(
                    "Bob", Users.PasswordFormat.SHA256, "abc123",
                    "bob@example.com", Collections.emptySet()
                ).yaml(),
                true
            ),
            new ImmutablePair<>(Yaml.createYamlMappingBuilder().build(), true),
            new ImmutablePair<>(Yaml.createYamlMappingBuilder().build(), false)
        );
    }

}
