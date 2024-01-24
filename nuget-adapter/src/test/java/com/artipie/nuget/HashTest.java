/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.metadata.PackageId;
import com.artipie.nuget.metadata.Version;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Hash}.
 *
 * @since 0.1
 */
class HashTest {

    @Test
    void shouldSave() {
        final String id = "abc";
        final String version = "0.0.1";
        final Storage storage = new InMemoryStorage();
        new Hash(new Content.From("abc123".getBytes())).save(
            storage,
            new PackageIdentity(new PackageId(id), new Version(version))
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            storage.value(new Key.From(id, version, "abc.0.0.1.nupkg.sha512"))
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::asciiString)
                .toCompletableFuture().join(),
            Matchers.equalTo("xwtd2ev7b1HQnUEytxcMnSB1CnhS8AaA9lZY8DEOgQBW5nY8NMmgCw6UAHb1RJXBafwjAszrMSA5JxxDRpUH3A==")
        );
    }
}
