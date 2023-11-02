/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.security.perms.PermissionConfig;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DockerRepositoryPermission}.
 * @since 0.18
 * @checkstyle MagicNumberCheck (300 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerRepositoryPermissionFactoryTest {

    @Test
    void createsPermissionCollection() throws IOException {
        MatcherAssert.assertThat(
            Collections.list(
                new DockerRepositoryPermissionFactory().newPermissions(
                    new PermissionConfig.FromYamlMapping(
                        Yaml.createYamlInput(
                            String.join(
                                "\n",
                                "docker-local:",
                                "  ubuntu-test:",
                                "    - *",
                                "www.boo.docker:",
                                "  alpine-slim:",
                                "    - pull",
                                "    - overwrite",
                                "  debian-postgres:",
                                "    - pull"
                            )
                        ).readYamlMapping()
                    )
                ).elements()
            ),
            Matchers.hasSize(3)
        );
    }

    @Test
    void createsPermissionCollectionWithOneItem() throws IOException {
        final ArrayList<? extends Permission> list = Collections.list(
            new DockerRepositoryPermissionFactory().newPermissions(
                new PermissionConfig.FromYamlMapping(
                    Yaml.createYamlInput(
                        String.join(
                            "\n",
                            "my-docker:",
                            "  alpine-slim:",
                            "    - *"
                        )
                    ).readYamlMapping()
                )
            ).elements()
        );
        MatcherAssert.assertThat(
            list, Matchers.hasSize(1)
        );
        MatcherAssert.assertThat(
            list.get(0),
            new IsEqual<>(
                new DockerRepositoryPermission("my-docker", "alpine-slim", DockerActions.ALL.mask())
            )
        );
    }

    @Test
    void createsPermissionCollectionWithAnyRepoAndAnyResource() throws IOException {
        final ArrayList<? extends Permission> list = Collections.list(
            new DockerRepositoryPermissionFactory().newPermissions(
                new PermissionConfig.FromYamlMapping(
                    Yaml.createYamlInput(
                        String.join(
                            "\n",
                            "\"*\":",
                            "  \"*\":",
                            "    - pull"
                        )
                    ).readYamlMapping()
                )
            ).elements()
        );
        MatcherAssert.assertThat(
            list, Matchers.hasSize(1)
        );
        MatcherAssert.assertThat(
            list.get(0),
            new IsEqual<>(
                new DockerRepositoryPermission("*", "*", DockerActions.PULL.mask())
            )
        );
    }

}
