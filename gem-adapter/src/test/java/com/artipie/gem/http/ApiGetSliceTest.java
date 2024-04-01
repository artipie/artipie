/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Content;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A test for gem submit operation.
 */
final class ApiGetSliceTest {
    @Test
    void queryResultsInOkResponse(@TempDir final Path tmp) throws IOException {
        new TestResource("gviz-0.3.5.gem").saveTo(tmp.resolve("./gviz-0.3.5.gem"));
        Response resp = new ApiGetSlice(new FileStorage(tmp))
            .response(
                new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.json"),
                Headers.EMPTY, Content.EMPTY
            ).join();
        Assertions.assertTrue(
            resp.body().asString().contains("\"name\":\"gviz\""),
            resp.body().asString()
        );
    }

    @Test
    void returnsValidResponseForYamlRequest(@TempDir final Path tmp) throws IOException {
        new TestResource("gviz-0.3.5.gem").saveTo(tmp.resolve("./gviz-0.3.5.gem"));
        Response resp = new ApiGetSlice(new FileStorage(tmp)).response(
            new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.yaml"),
            Headers.EMPTY, Content.EMPTY
        ).join();
        Assertions.assertEquals(
            "text/x-yaml; charset=utf-8",
            resp.headers().single("Content-Type").getValue()
        );
        Assertions.assertTrue(
            resp.body().asString().contains("name: gviz")
        );
    }
}

