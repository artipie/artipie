/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link DockerRegistryPermission}.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerRegistryPermissionTest {

    @Test
    void permissionWithAnyCategoryIsNotImplied() {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("my-docker", RegistryCategory.BASE.mask()).implies(
                new DockerRegistryPermission("my-docker", RegistryCategory.ANY.mask())
            ),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            new DockerRegistryPermission("my-docker", RegistryCategory.CATALOG.mask()).implies(
                new DockerRegistryPermission("my-docker", RegistryCategory.ANY.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionWithWildcardIsNotImplied() {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("my-docker", RegistryCategory.BASE.mask()).implies(
                new DockerRegistryPermission("*", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionsWithWildCardNameImpliesAnyName() {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("*", RegistryCategory.BASE.mask()).implies(
                new DockerRegistryPermission("my-docker", RegistryCategory.BASE.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            new DockerRegistryPermission("*", RegistryCategory.CATALOG.mask()).implies(
                new DockerRegistryPermission("docker-local", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @EnumSource(RegistryCategory.class)
    void permissionsWithAnyCategoriesImpliesAnyCategory(final RegistryCategory item) {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("docker-global", RegistryCategory.ANY.mask()).implies(
                new DockerRegistryPermission("docker-global", item.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void permissionsWithCategoriesNamesAreNotImplied() {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("my-docker", RegistryCategory.BASE.mask()).implies(
                new DockerRegistryPermission("my-docker", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionsWithDifferentNamesAreNotImplied() {
        MatcherAssert.assertThat(
            new DockerRegistryPermission("my-docker", RegistryCategory.CATALOG.mask()).implies(
                new DockerRegistryPermission("docker-local", RegistryCategory.CATALOG.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void impliesItself() {
        final DockerRegistryPermission perm =
            new DockerRegistryPermission("my-docker", RegistryCategory.CATALOG.mask());
        MatcherAssert.assertThat(
            perm.implies(perm),
            new IsEqual<>(true)
        );
    }

}
