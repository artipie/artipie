/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.perms;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DockerRegistryPermission.DockerRegistryPermissionCollection}.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerRegistryPermissionCollectionTest {

    /**
     * Test collection.
     */
    private DockerRegistryPermission.DockerRegistryPermissionCollection collection;

    @BeforeEach
    void init() {
        this.collection = new DockerRegistryPermission.DockerRegistryPermissionCollection();
    }

    @Test
    void impliesConcretePermissions() {
        this.collection.add(
            new DockerRegistryPermission("my-repo", RegistryCategory.CATALOG.mask())
        );
        this.collection.add(
            new DockerRegistryPermission("docker-local", RegistryCategory.BASE.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("my-repo", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("docker-local", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void impliesWhenAllPermissionIsPresent() {
        this.collection.add(new DockerRegistryPermission("*", RegistryCategory.ANY.mask()));
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("docker-local", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("my-repo", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void impliesWhenAnyNamePermissionIsPresent() {
        this.collection.add(
            new DockerRegistryPermission("*", RegistryCategory.CATALOG.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("my-repo", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void notImpliesPermissionWithAnotherName() {
        this.collection.add(
            new DockerRegistryPermission("docker-local", RegistryCategory.CATALOG.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("my-repo", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void notImpliesPermissionWithAnotherAction() {
        this.collection.add(
            new DockerRegistryPermission("docker-local", RegistryCategory.CATALOG.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("docker-local", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void emptyCollectionDoesNotImply() {
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRegistryPermission("my-repo", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(false)
        );
    }

}
