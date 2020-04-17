/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.fs.RxFile;
import io.vertx.reactivex.core.Vertx;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoConfig}.
 * @since 0.2
 */
public class RepoConfigTest {
    @Test
    public void readsCustom() throws URISyntaxException, ExecutionException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        try {
            final RxFile file = new RxFile(
                Paths.get(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("repo-config.yml")
                        .toURI()
                ),
                vertx.fileSystem()
            );
            final RepoConfig config = new RepoConfig(file.flow());
            final YamlMapping yaml = config.custom().toCompletableFuture().get();
            MatcherAssert.assertThat(
                yaml.string("custom-property"),
                new IsEqual<>("custom-value")
            );
        } finally {
            vertx.close();
        }
    }
}
