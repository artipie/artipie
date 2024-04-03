/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.artipie.docker.misc.Pagination;
import com.google.common.base.Strings;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link Pagination}.
 */
class CatalogPaginationTest {

    @ParameterizedTest
    @CsvSource({
        ",0x7fffffff,/v2/_catalog",
        "some/image,0x7fffffff,/v2/_catalog?last=some/image",
        ",10,/v2/_catalog?n=10",
        "my-alpine,20,/v2/_catalog?n=20&last=my-alpine"
    })
    void shouldBuildPathString(String repo, int limit, String uri) {
        Pagination p = new Pagination(
            Strings.isNullOrEmpty(repo) ? null : new RepoName.Simple(repo),
            limit
        );
        MatcherAssert.assertThat(
            URLDecoder.decode(p.uriWithPagination("/v2/_catalog"), StandardCharsets.UTF_8),
            Matchers.is(uri)
        );
    }
}
