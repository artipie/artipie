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

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Slice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.rpm.http.RpmSlice;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SliceFromConfig}.
 * @since 0.2
 */
class SliceFromConfigTest {

    /**
     * Vertx instance.
     */
    private Vertx vertx;

    @Test
    void returnsMavenSliceForMavenRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("maven"),
            new IsInstanceOf(MavenSlice.class)
        );
    }

    @Test
    void returnsFileSliceForFileRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("file"),
            new IsInstanceOf(FilesSlice.class)
        );
    }

    @Test
    void returnsNpmSliceForNpmRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("npm"),
            new IsInstanceOf(NpmSlice.class)
        );
    }

    @Test
    void returnsRpmSliceForRpmRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("rpm"),
            new IsInstanceOf(RpmSlice.class)
        );
    }

    @Test
    void returnsPhpSliceForPhpRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("php"),
            new IsInstanceOf(PhpComposer.class)
        );
    }

    @Test
    void returnsGoSliceForGoRepo() throws Exception {
        MatcherAssert.assertThat(
            this.buildSlice("go"),
            new IsInstanceOf(GoSlice.class)
        );
    }

    @BeforeEach
    void setUp() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    /**
     * Creates repo config.
     * @param type Repo type
     * @return Config
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Slice buildSlice(final String type) throws Exception {
        return SliceFromConfig.build(
            new RepoConfig(
                "",
                Flowable.just(
                    ByteBuffer.wrap(
                        Yaml.createYamlMappingBuilder()
                            .add(
                                "repo",
                                Yaml.createYamlMappingBuilder()
                                    .add("type", type)
                                    .add("path", "some")
                                    .add(
                                        "storage",
                                        Yaml.createYamlMappingBuilder()
                                            .add("type", "fs")
                                            .add("path", "/opt/storage").build()
                                    ).build()
                            ).build().toString().getBytes()
                    )
                )
            ),
            this.vertx,
            (user, pass) -> Optional.of(user)
        ).toCompletableFuture().get();
    }
}
