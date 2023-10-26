/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link CatalogUri}.
 *
 * @since 0.10
 */
class CatalogUriTest {

    @ParameterizedTest
    @CsvSource({
        ",0x7fffffff,/v2/_catalog",
        "some/image,0x7fffffff,/v2/_catalog?last=some/image",
        ",10,/v2/_catalog?n=10",
        "my-alpine,20,/v2/_catalog?n=20&last=my-alpine"
    })
    void shouldBuildPathString(final String repo, final int limit, final String uri) {
        MatcherAssert.assertThat(
            new CatalogUri(
                Optional.ofNullable(repo).map(RepoName.Simple::new),
                limit
            ).string(),
            new IsEqual<>(uri)
        );
    }
}
