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
 * Test for {@link DockerRegistryPermissionFactory}.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerRegistryPermissionFactoryTest {

    @Test
    void createsPermissionCollection() throws IOException {
        MatcherAssert.assertThat(
            Collections.list(
                new DockerRegistryPermissionFactory().newPermissions(
                    new PermissionConfig.FromYamlMapping(
                        Yaml.createYamlInput(
                            String.join(
                                "\n",
                                "docker-local:",
                                "  - *",
                                "www.boo.docker:",
                                "  - base"
                            )
                        ).readYamlMapping()
                    )
                ).elements()
            ),
            Matchers.hasSize(2)
        );
    }

    @Test
    void createsPermissionCollectionWithOneItem() throws IOException {
        final ArrayList<? extends Permission> list = Collections.list(
            new DockerRegistryPermissionFactory().newPermissions(
                new PermissionConfig.FromYamlMapping(
                    Yaml.createYamlInput(
                        String.join(
                            "\n",
                            "my-docker:",
                            "  - catalog",
                            "  - base"
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
                new DockerRegistryPermission("my-docker", RegistryCategory.ANY.mask())
            )
        );
    }

}
