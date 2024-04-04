/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.perms.RegistryCategory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Scope}.
 *
 * @since 0.10
 */
class ScopeTest {

    @Test
    void repositoryPullScope() {
        MatcherAssert.assertThat(
            new Scope.Repository.Pull("samalba/my-app").string(),
            new IsEqual<>("repository:samalba/my-app:PULL")
        );
    }

    @Test
    void repositoryPushScope() {
        MatcherAssert.assertThat(
            new Scope.Repository.Push("busybox").string(),
            new IsEqual<>("repository:busybox:PUSH")
        );
    }

    @Test
    void registryScope() {
        MatcherAssert.assertThat(
            new Scope.Registry(RegistryCategory.BASE).string(),
            new IsEqual<>("registry:*:BASE")
        );
    }
}
