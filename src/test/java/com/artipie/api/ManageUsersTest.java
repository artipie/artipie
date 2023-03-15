/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.users.CrudUsers;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArray;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link ManageUsers}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManageUsersTest {

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
        this.users = new ManageUsers(this.blsto);
    }

    @Test
    void listUsers() throws JSONException {
        this.blsto.save(
            new Key.From("users/Alice.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: qwerty",
                "roles:",
                "  - readers",
                "permissions:",
                "  adapter_basic_permission:",
                "    repo1:",
                "      - write"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.blsto.save(
            new Key.From("users/Bob.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: qwerty",
                "email: bob@example.com",
                "roles:",
                "  - admin"
            ).getBytes(StandardCharsets.UTF_8)
        );
        JSONAssert.assertEquals(
            this.users.list().toString(),
            // @checkstyle LineLengthCheck (1 line)
            "[{\"name\":\"Alice\",\"roles\":[\"readers\"], \"permissions\":{\"adapter_basic_permission\":{\"repo1\":[\"write\"]}}},{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"roles\":[\"admin\"]}]",
            true
        );
    }

    @Test
    void returnsEmptyList() {
        MatcherAssert.assertThat(
            this.users.list(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void addsNewUser() {
        final String alice = "Alice";
        final String email = "Alice@example.com";
        this.users.addOrUpdate(
            Json.createObjectBuilder().add("type", "plain").add("pass", "xyz")
                .add("email", email)
                .add("roles", Json.createArrayBuilder().add("reader").add("creator").build())
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
            new String(this.blsto.value(new Key.From("users/Alice.yml")), StandardCharsets.UTF_8),
            new StringContains(
                String.join(
                    System.lineSeparator(),
                    "type: plain",
                    "pass: xyz",
                    "email: Alice@example.com",
                    "roles:",
                    "  - reader",
                    "  - creator"
                )
            )
        );
    }

    @Test
    void replacesUser() {
        this.blsto.save(
            new Key.From("users/Alice.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: 025",
                "email: abc@example.com",
                "roles:",
                "  - java-dev"
            ).getBytes(StandardCharsets.UTF_8)
        );
        final String alice = "Alice";
        final String email = "Alice@example.com";
        this.users.addOrUpdate(
            Json.createObjectBuilder().add("type", "plain").add("pass", "xyz")
                .add("email", email)
                .add("roles", Json.createArrayBuilder().add("reader").add("creator").build())
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
            new String(this.blsto.value(new Key.From("users/Alice.yaml")), StandardCharsets.UTF_8),
            new StringContains(
                String.join(
                    System.lineSeparator(),
                    "type: plain",
                    "pass: xyz",
                    "email: Alice@example.com",
                    "roles:",
                    "  - reader",
                    "  - creator"
                )
            )
        );
    }

    @Test
    void throwsErrorWhenUserNotExist() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.users.remove("Alice")
        );
    }

    @Test
    void removesUser() {
        this.blsto.save(new Key.From("users/john.yaml"), new byte[]{});
        this.users.remove("john");
        MatcherAssert.assertThat(
            this.users.list().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void altersPassword() {
        this.blsto.save(
            new Key.From("users/John.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: bdhdb",
                "email: john@example.com",
                "roles:",
                "  - java-dev",
                "permissions:",
                "  adapter_basic_permission:",
                "    repo1:",
                "      - write"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.users.alterPassword(
            "John",
            Json.createObjectBuilder().add("new_pass", "[poiu").add("new_type", "plain").build()
        );
        MatcherAssert.assertThat(
            new String(this.blsto.value(new Key.From("users/John.yaml")), StandardCharsets.UTF_8),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "type: plain",
                    "pass: \"[poiu\"",
                    "email: john@example.com",
                    "roles:",
                    "  - \"java-dev\"",
                    "permissions:",
                    "  adapter_basic_permission:",
                    "    repo1:",
                    "      - write"
                )
            )
        );
    }

    @Test
    void enablesDisabledUser() {
        this.blsto.save(
            new Key.From("users/John.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: bdhdb",
                "email: john@example.com",
                "enabled: false"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.users.enable("John");
        MatcherAssert.assertThat(
            new String(this.blsto.value(new Key.From("users/John.yaml")), StandardCharsets.UTF_8),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "type: plain",
                    "pass: bdhdb",
                    "email: john@example.com",
                    "enabled: true"
                )
            )
        );
    }

    @Test
    void disablesUser() {
        this.blsto.save(
            new Key.From("users/John.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: bdhdb",
                "email: john@example.com"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.users.disable("John");
        MatcherAssert.assertThat(
            new String(this.blsto.value(new Key.From("users/John.yaml")), StandardCharsets.UTF_8),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "type: plain",
                    "pass: bdhdb",
                    "email: john@example.com",
                    "enabled: false"
                )
            )
        );
    }

}
