/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link TagsListUri}.
 *
 * @since 0.10
 * @checkstyle ParameterNumberCheck (500 lines)
 */
class TagsListUriTest {

    @ParameterizedTest
    @CsvSource({
        "library/busybox,,0x7fffffff,/v2/library/busybox/tags/list",
        "my-image,latest,0x7fffffff,/v2/my-image/tags/list?last=latest",
        "dotnet/runtime,,10,/v2/dotnet/runtime/tags/list?n=10",
        "my-alpine,1.0,20,/v2/my-alpine/tags/list?n=20&last=1.0"
    })
    void shouldBuildPathString(
        final String repo, final String from, final int limit, final String uri
    ) {
        MatcherAssert.assertThat(
            new TagsListUri(
                new RepoName.Simple(repo),
                Optional.ofNullable(from).map(Tag.Valid::new),
                limit
            ).string(),
            new IsEqual<>(uri)
        );
    }
}
