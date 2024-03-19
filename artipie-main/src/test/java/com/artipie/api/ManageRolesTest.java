/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.users.CrudRoles;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArray;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link ManageRoles}.
 */
class ManageRolesTest {

    private BlockingStorage blsto;

    private CrudRoles roles;

    @BeforeEach
    void init() {
        this.blsto = new BlockingStorage(new InMemoryStorage());
        this.roles = new ManageRoles(this.blsto);
    }

    @Test
    void listUsers() throws JSONException {
        this.blsto.save(
            new Key.From("roles/java-dev.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permissions:",
                "    maven:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.blsto.save(
            new Key.From("roles/readers.yml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permissions:",
                "    \"*\":",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        JSONAssert.assertEquals(
            this.roles.list().toString(),
            "[{\"name\":\"java-dev\",\"permissions\":{\"adapter_basic_permissions\":{\"maven\":[\"write\",\"read\"]}}},{\"name\":\"readers\",\"permissions\":{\"adapter_basic_permissions\":{\"*\":[\"read\"]}}}]",
            true
        );
    }

    @Test
    void returnsEmptyList() {
        MatcherAssert.assertThat(
            this.roles.list(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void addsNewRole() {
        final String devs = "pypi-dev";
        this.roles.addOrUpdate(
            Json.createObjectBuilder().add(
                "permissions",
                Json.createObjectBuilder().add(
                    "adapter_basic_permissions",
                    Json.createObjectBuilder()
                        .add("main-pypi", Json.createArrayBuilder().add("read"))
                        .add("test-pypi", Json.createArrayBuilder().add("read").add("write"))
                )
            ).build(),
            devs
        );
        MatcherAssert.assertThat(
            "Failed to find added role",
            this.roles.list().stream()
                .anyMatch(usr -> devs.equals(usr.asJsonObject().getString("name")))
        );
        MatcherAssert.assertThat(
            "Failed to add role",
            new String(
                this.blsto.value(new Key.From("roles/pypi-dev.yml")), StandardCharsets.UTF_8
            ),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "permissions:",
                    "  adapter_basic_permissions:",
                    "    \"main-pypi\":",
                    "      - read",
                    "    \"test-pypi\":",
                    "      - read",
                    "      - write"
                )
            )
        );
    }

    @Test
    void replacesRole() {
        this.blsto.save(
            new Key.From("roles/testers.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permissions:",
                "    test-repo:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        final String testers = "testers";
        this.roles.addOrUpdate(
            Json.createObjectBuilder().add(
                "permissions",
                Json.createObjectBuilder().add(
                    "adapter_basic_permissions",
                    Json.createObjectBuilder()
                        .add("test-maven", Json.createArrayBuilder().add("read"))
                        .add("test-pypi", Json.createArrayBuilder().add("read").add("write"))
                )
            ).build(),
            testers
        );
        final JsonArray list = this.roles.list();
        MatcherAssert.assertThat(
            "Failed to find added role",
            list.stream().anyMatch(usr -> testers.equals(usr.asJsonObject().getString("name")))
        );
        MatcherAssert.assertThat(
            "Failed to add role",
            new String(
                this.blsto.value(new Key.From("roles/testers.yaml")), StandardCharsets.UTF_8
            ),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "permissions:",
                    "  adapter_basic_permissions:",
                    "    \"test-maven\":",
                    "      - read",
                    "    \"test-pypi\":",
                    "      - read",
                    "      - write"
                )
            )
        );
    }

    @Test
    void throwsErrorWhenUserNotExist() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.roles.remove("admins")
        );
    }

    @Test
    void removesRole() {
        this.blsto.save(new Key.From("roles/dev.yaml"), new byte[]{});
        this.roles.remove("dev");
        MatcherAssert.assertThat(
            this.roles.list().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void enablesDisabledRole() {
        this.blsto.save(
            new Key.From("roles/testers.yaml"),
            String.join(
                System.lineSeparator(),
                "enabled: false",
                "permissions:",
                "  adapter_basic_permissions:",
                "    test-repo:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.roles.enable("testers");
        MatcherAssert.assertThat(
            new String(
                this.blsto.value(new Key.From("roles/testers.yaml")), StandardCharsets.UTF_8
            ),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "enabled: true",
                    "permissions:",
                    "  adapter_basic_permissions:",
                    "    \"test-repo\":",
                    "      - write",
                    "      - read"
                )
            )
        );
    }

    @Test
    void disablesRole() {
        this.blsto.save(
            new Key.From("roles/testers.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permissions:",
                "    test-repo:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.roles.disable("testers");
        MatcherAssert.assertThat(
            new String(
                this.blsto.value(new Key.From("roles/testers.yaml")), StandardCharsets.UTF_8
            ),
            new IsEqual<>(
                String.join(
                    System.lineSeparator(),
                    "permissions:",
                    "  adapter_basic_permissions:",
                    "    \"test-repo\":",
                    "      - write",
                    "      - read",
                    "enabled: false"
                )
            )
        );
    }

}
