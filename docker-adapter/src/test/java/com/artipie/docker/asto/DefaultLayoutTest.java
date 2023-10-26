/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.docker.RepoName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DefaultLayout}.
 *
 * @since 0.8
 */
public final class DefaultLayoutTest {

    @Test
    public void buildsRepositories() {
        MatcherAssert.assertThat(
            new DefaultLayout().repositories().string(),
            new IsEqual<>("repositories")
        );
    }

    @Test
    public void buildsTags() {
        MatcherAssert.assertThat(
            new DefaultLayout().tags(new RepoName.Simple("my-alpine")).string(),
            new IsEqual<>("repositories/my-alpine/_manifests/tags")
        );
    }
}
