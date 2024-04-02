/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoRepo;
import com.artipie.docker.asto.Uploads;
import com.artipie.docker.asto.Layout;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReadWriteRepo}.
 *
 * @since 0.3
 */
final class ReadWriteRepoTest {

    @Test
    void createsReadWriteLayers() {
        MatcherAssert.assertThat(
            new ReadWriteRepo(repo(), repo()).layers(),
            new IsInstanceOf(ReadWriteLayers.class)
        );
    }

    @Test
    void createsReadWriteManifests() {
        MatcherAssert.assertThat(
            new ReadWriteRepo(repo(), repo()).manifests(),
            new IsInstanceOf(ReadWriteManifests.class)
        );
    }

    @Test
    void createsWriteUploads() {
        final Uploads uploads = new Uploads(
            new InMemoryStorage(),
            new Layout(),
            new RepoName.Simple("test")
        );
        MatcherAssert.assertThat(
            new ReadWriteRepo(
                repo(),
                new Repo() {
                    @Override
                    public Layers layers() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Manifests manifests() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Uploads uploads() {
                        return uploads;
                    }
                }
            ).uploads(),
            new IsEqual<>(uploads)
        );
    }

    private static Repo repo() {
        return new AstoRepo(
            new InMemoryStorage(),
            new Layout(),
            new RepoName.Simple("test-repo")
        );
    }
}
