/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PermissionConfig.FromYamlMapping}.
 * Yaml permissions format example:
 * <pre>{@code
 *   # permissions for some role
 *
 *   java-devs:
 *     adapter_basic_permissions:
 *       maven-repo:
 *         - read
 *         - write
 *       python-repo:
 *         - read
 *       npm-repo:
 *         - read
 *
 *   # permissions for admin
 *   admins:
 *     all_permission: {}
 * }</pre>
 * {@link PermissionConfig.FromYamlMapping} implementation will receive mapping for
 * single permission adapter_basic_permissions instance, for example:
 * <pre>{@code
 * maven-repo:
 *   - read
 *   - write
 * }</pre>
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PermissionConfigFromYamlMappingTest {

    @Test
    void readsSequence() throws IOException {
        MatcherAssert.assertThat(
            new PermissionConfig.FromYamlMapping(
                Yaml.createYamlInput(
                    String.join(
                        "\n",
                        "some-repo:",
                        "  - read",
                        "  - \"*\"",
                        "  - delete"
                    )
                ).readYamlMapping()
            ).sequence("some-repo"),
            Matchers.contains("read", "*", "delete")
        );
    }

    @Test
    void readSubConfigAndValueByKey() throws IOException {
        MatcherAssert.assertThat(
            new PermissionConfig.FromYamlMapping(
                Yaml.createYamlInput(
                    String.join(
                        "\n",
                        "some-repo:",
                        "  key: value",
                        "  key1: value2"
                    )
                ).readYamlMapping()
            ).config("some-repo").string("key"),
            new IsEqual<>("value")
        );
    }

    @Test
    void returnsKeys() throws IOException {
        final YamlMapping yaml = Yaml.createYamlInput(
            String.join(
                "\n",
                "docker-repo:",
                "  my-alpine:",
                "    - pull",
                "  ubuntu-slim:",
                "    - pull",
                "    - push",
                "docker-local:",
                "  my-alpine:",
                "    - pull",
                "  ubuntu-slim:",
                "    - pull",
                "    - push",
                "docker-vasy:",
                "  vasy-img:",
                "    - pull"
            )
        ).readYamlMapping();
        MatcherAssert.assertThat(
            new PermissionConfig.FromYamlMapping(yaml).keys(),
            Matchers.contains("docker-repo", "docker-local", "docker-vasy")
        );
    }

    @Test
    void returnsSubConfig() throws IOException {
        final PermissionConfig conf = new PermissionConfig.FromYamlMapping(
            Yaml.createYamlInput(
                String.join(
                    "\n",
                    "\"*\":",
                    "  \"*\":",
                    "    - pull"
                )
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            conf.keys(),
            Matchers.contains("*")
        );
        final PermissionConfig sub = (PermissionConfig) conf.config("*");
        MatcherAssert.assertThat(
            sub.keys(),
            Matchers.contains("*")
        );
        MatcherAssert.assertThat(
            sub.sequence("*"),
            Matchers.contains("pull")
        );
    }
}
