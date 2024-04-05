/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.security.perms.PermissionConfig;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Test for {@link DockerRegistryPermissionFactory}.
 */
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
                                "  - base",
                                "  - *"
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
            list.getFirst(),
            Matchers.is(
                new DockerRegistryPermission("my-docker", RegistryCategory.ANY.mask())
            )
        );
    }

}
