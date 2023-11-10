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
 * Test for {@link DockerRepositoryPermission}.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerRepositoryPermissionCollectionTest {

    /**
     * Test collection.
     */
    private DockerRepositoryPermission.DockerRepositoryPermissionCollection collection;

    @BeforeEach
    void init() {
        this.collection = new DockerRepositoryPermission.DockerRepositoryPermissionCollection();
    }

    @Test
    void impliesConcretePermissions() {
        this.collection.add(
            new DockerRepositoryPermission("my-repo", "linux", DockerActions.PUSH.mask())
        );
        this.collection.add(
            new DockerRepositoryPermission("some-repo", "ubuntu-slim", DockerActions.PULL.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission(
                    "some-repo", "ubuntu-slim", DockerActions.PULL.mask()
                )
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "linux", DockerActions.PUSH.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void impliesWhenAllPermissionIsPresent() {
        this.collection.add(
            new DockerRepositoryPermission("*", "*", DockerActions.ALL.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "ubuntu-slim", DockerActions.PULL.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("some-repo", "linux", DockerActions.PUSH.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void impliesWhenAnyNamePermissionIsPresent() {
        this.collection.add(
            new DockerRepositoryPermission("*", "centos", DockerActions.PUSH.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "centos", DockerActions.PUSH.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("some-repo", "centos", DockerActions.PUSH.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void impliesWhenAnyResourcePermissionIsPresent() {
        this.collection.add(
            new DockerRepositoryPermission("my-repo", "*", DockerActions.PULL.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "centos", DockerActions.PULL.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "ubuntu-slim", DockerActions.PULL.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void permissionsWithAnyResourceAndAnyNameWorks() {
        this.collection.add(
            new DockerRepositoryPermission("*", "*", DockerActions.PULL.mask())
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "linux", DockerActions.PULL.mask())
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("local-repo", "alpine", DockerActions.PULL.mask())
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void emptyCollectionDoesNotImply() {
        MatcherAssert.assertThat(
            this.collection.implies(
                new DockerRepositoryPermission("my-repo", "linux", DockerActions.PUSH.mask())
            ),
            new IsEqual<>(false)
        );
    }

}
