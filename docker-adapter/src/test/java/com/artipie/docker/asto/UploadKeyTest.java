/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.docker.RepoName;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link UploadKey}.
 *
 * @since 0.3
 */
public final class UploadKeyTest {

    @Test
    public void shouldBuildExpectedString() {
        final String name = "test";
        final String uuid = UUID.randomUUID().toString();
        MatcherAssert.assertThat(
            new UploadKey(new RepoName.Valid(name), uuid).string(),
            Matchers.equalTo(
                String.format("repositories/%s/_uploads/%s", name, uuid)
            )
        );
    }
}
