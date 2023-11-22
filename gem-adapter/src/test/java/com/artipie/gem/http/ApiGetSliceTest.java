/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.IsJson;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wtf.g4s8.hamcrest.json.JsonHas;

/**
 * A test for gem submit operation.
 *
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ApiGetSliceTest {
    @Test
    void queryResultsInOkResponse(@TempDir final Path tmp) throws IOException {
        new TestResource("gviz-0.3.5.gem").saveTo(tmp.resolve("./gviz-0.3.5.gem"));
        MatcherAssert.assertThat(
            new ApiGetSlice(new FileStorage(tmp)),
            new SliceHasResponse(
                new RsHasBody(new IsJson(new JsonHas("name", "gviz"))),
                new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.json"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }

    @Test
    void returnsValidResponseForYamlRequest(@TempDir final Path tmp) throws IOException {
        new TestResource("gviz-0.3.5.gem").saveTo(tmp.resolve("./gviz-0.3.5.gem"));
        MatcherAssert.assertThat(
            new ApiGetSlice(new FileStorage(tmp)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasHeaders(
                        Matchers.equalTo(
                            new Header("Content-Type", "text/x-yaml;charset=utf-8")
                        ),
                        Matchers.anything()
                    ),
                    new RsHasBody(new StringContains("name: gviz"), StandardCharsets.UTF_8)
                ),
                new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.yaml"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }
}

