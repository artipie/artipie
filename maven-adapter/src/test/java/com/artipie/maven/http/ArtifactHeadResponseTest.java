/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ArtifactGetResponse}.
 *
 * @since 0.5
 */
final class ArtifactHeadResponseTest {

    @Test
    void okIfArtifactExists() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("repo/artifact.jar");
        new BlockingStorage(storage).save(key, "something".getBytes());
        MatcherAssert.assertThat(
            new ArtifactHeadResponse(storage, key),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void noBodyIfExists() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("repo/artifact2.jar");
        new BlockingStorage(storage).save(key, "data".getBytes(StandardCharsets.UTF_8));
        MatcherAssert.assertThat(
            new ArtifactHeadResponse(storage, key),
            new RsHasBody(new byte[0])
        );
    }

    @Test
    void notFoundIfDoesnExist() {
        final Storage storage = new InMemoryStorage();
        MatcherAssert.assertThat(
            new ArtifactHeadResponse(storage, new Key.From("none")),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }
}
