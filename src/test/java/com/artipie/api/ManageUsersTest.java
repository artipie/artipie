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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
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
    private static final Key KEY = new Key.From("creds.yaml");

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
            "[{\"name\":\"Alice\",\"pass\":123,\"type\":\"plain\",\"groups\":[\"readers\"]},{\"name\":\"Bob\",\"type\":\"plain\",\"pass\":\"xyz\",\"email\":\"bob@example.com\",\"groups\":[\"admin\"]}]",
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
        final String pass = "xyz";
        this.users.add(
            Json.createObjectBuilder().add("type", "plain").add("pass", pass)
                .add("email", email)
                .add("groups", Json.createArrayBuilder().add("reader").add("creator").build())
                .build(),
            alice
        );
        final JsonArray list = this.users.list();
        final JsonObject nuser = list.stream()
            .filter(usr -> alice.equals(usr.asJsonObject().getString("name")))
            .findFirst().get().asJsonObject();
        MatcherAssert.assertThat(
            "Failed to add user email",
            nuser.getString("email"),
            new IsEqual<>(email)
        );
        MatcherAssert.assertThat(
            "Failed to add password",
            nuser.getString("pass").toString().equals(pass)
        );
        MatcherAssert.assertThat(
            "Failed to add groups",
            nuser.get("groups").asJsonArray().stream().map(JsonValue::toString)
                .map(item -> item.replace("\"", "")).collect(Collectors.toList()),
            Matchers.containsInAnyOrder("reader", "creator")
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
