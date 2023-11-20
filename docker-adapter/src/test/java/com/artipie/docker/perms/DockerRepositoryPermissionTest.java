/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link DockerRepositoryPermission}.
 * @since 0.18
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UnnecessaryAnnotationValueElement"})
class DockerRepositoryPermissionTest {

    @Test
    void permissionsWithDifferentNamesAreNotImplied() {
        final String resource = "my-centos";
        final int mask = DockerActions.PULL.mask();
        MatcherAssert.assertThat(
            new DockerRepositoryPermission("repo1", resource, mask).implies(
                new DockerRepositoryPermission("repo2", resource, mask)
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionsWithDifferentResourcesAreNotImplied() {
        final String name = "repo";
        final int mask = DockerActions.PULL.mask();
        MatcherAssert.assertThat(
            new DockerRepositoryPermission(name, "ubuntu", mask).implies(
                new DockerRepositoryPermission(name, "centos", mask)
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionsWithDifferentActionsAreNotImplied() {
        final String name = "repo";
        final String resource = "linux";
        MatcherAssert.assertThat(
            new DockerRepositoryPermission(name, resource, DockerActions.PULL.mask()).implies(
                new DockerRepositoryPermission(name, resource, DockerActions.PUSH.mask())
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @EnumSource(value = DockerActions.class)
    void permissionsWithActionAllImpliesAnyOtherAction(final DockerActions action) {
        final String name = "repo";
        final String resource = "linux";
        MatcherAssert.assertThat(
            new DockerRepositoryPermission(name, resource, DockerActions.ALL.mask()).implies(
                new DockerRepositoryPermission(name, resource, action.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "pull,push;overwrite;*",
        "push,pull;overwrite;*",
        "overwrite,push;pull;*"
    })
    void permissionsWithActionNotImpliesOtherAction(final String action, final String implies) {
        final String name = "repo";
        final String resource = "linux";
        MatcherAssert.assertThat(
            new DockerRepositoryPermission(name, resource, DockerActions.maskByAction(action))
                .implies(
                    new DockerRepositoryPermission(
                        name, resource, Stream.of(implies.split(";")).collect(Collectors.toSet())
                    )
                ),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionWithAnyNameWorksCorrectly() {
        final String resource = "linux";
        final int mask = DockerActions.PULL.mask();
        final DockerRepositoryPermission anyname =
            new DockerRepositoryPermission("*", resource, mask);
        final DockerRepositoryPermission myrepo =
            new DockerRepositoryPermission("myrepo", resource, mask);
        MatcherAssert.assertThat(
            anyname.implies(myrepo),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            myrepo.implies(anyname),
            new IsEqual<>(false)
        );
    }

    @Test
    void permissionWithAnyResourceWorksCorrectly() {
        final String name = "docker-repo";
        final int mask = DockerActions.PUSH.mask();
        final DockerRepositoryPermission anyresource =
            new DockerRepositoryPermission(name, "*", mask);
        final DockerRepositoryPermission ubuntu =
            new DockerRepositoryPermission(name, "ubuntu", mask);
        MatcherAssert.assertThat(
            anyresource.implies(ubuntu),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            ubuntu.implies(anyresource),
            new IsEqual<>(false)
        );
    }

}
