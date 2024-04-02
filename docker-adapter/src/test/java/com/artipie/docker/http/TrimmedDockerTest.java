/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.Uploads;
import com.artipie.docker.fake.FakeCatalogDocker;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Test for {@link TrimmedDocker}.
 */
class TrimmedDockerTest {

    /**
     * Fake docker.
     */
    private static final Docker FAKE = new Docker() {
        @Override
        public Repo repo(final RepoName name) {
            return new FakeRepo(name);
        }

        @Override
        public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void failsIfPrefixNotFound() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new TrimmedDocker(TrimmedDockerTest.FAKE, "abc/123")
                .repo(new RepoName.Simple("xfe/oiu"))
        );
    }

    @ParameterizedTest
    @CsvSource({
        "one,two/three",
        "one/two,three",
        "v2/library/ubuntu,username/project_one",
        "v2/small/repo/,username/11/some.package",
        ",username/11/some_package"
    })
    void cutsIfPrefixStartsWithSlash(final String prefix, final String name) {
        Assertions.assertEquals(
            name,
            ((FakeRepo) new TrimmedDocker(TrimmedDockerTest.FAKE, prefix)
                .repo(new RepoName.Simple(prefix + '/' + name))).name()
        );
    }

    @Test
    void trimsCatalog() {
        final Optional<RepoName> from = Optional.of(new RepoName.Simple("foo/bar"));
        final int limit = 123;
        final Catalog catalog = () -> new Content.From(
            "{\"repositories\":[\"one\",\"two\"]}".getBytes()
        );
        final FakeCatalogDocker fake = new FakeCatalogDocker(catalog);
        final TrimmedDocker docker = new TrimmedDocker(fake, "foo");
        final Catalog result = docker.catalog(from, limit).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Forwards from without prefix",
            fake.from().map(RepoName::value),
            new IsEqual<>(Optional.of("bar"))
        );
        MatcherAssert.assertThat(
            "Forwards limit",
            fake.limit(),
            new IsEqual<>(limit)
        );
        MatcherAssert.assertThat(
            "Returns catalog with prefixes",
            result.json().asString(),
            new StringIsJson.Object(
                new JsonHas(
                    "repositories",
                    new JsonContains(
                        new JsonValueIs("foo/one"), new JsonValueIs("foo/two")
                    )
                )
            )
        );
    }

    /**
     * Fake repo.
     * @since 0.4
     */
    static final class FakeRepo implements Repo {

        /**
         * Repo name.
         */
        private final RepoName rname;

        /**
         * Ctor.
         * @param name Repo name
         */
        FakeRepo(final RepoName name) {
            this.rname = name;
        }

        @Override
        public Layers layers() {
            return null;
        }

        @Override
        public Manifests manifests() {
            return null;
        }

        @Override
        public Uploads uploads() {
            return null;
        }

        /**
         * Name of the repo.
         * @return Name
         */
        public String name() {
            return this.rname.value();
        }
    }

}
