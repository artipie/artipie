/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Test case for {@link RepositoryChecksums}.
 */
final class RepositoryChecksumsTest {

    @Test
    void findsArtifactChecksums() throws Exception {
        final Storage storage = new InMemoryStorage();
        final BlockingStorage bsto = new BlockingStorage(storage);
        final Key.From artifact = new Key.From("com/test/1.0/my-package.jar");
        bsto.save(artifact, "artifact".getBytes());
        final String sha1 = "c71de136f9377eca14b4218cc7001c8060c6974f";
        bsto.save(
            new Key.From("com/test/1.0/my-package.jar.sha1"),
            sha1.getBytes(StandardCharsets.UTF_8)
        );
        final String sha256 = "62090f2241986a8361242e47cf541657099fdccc0c08e34cd694922bdcf31893";
        bsto.save(
            new Key.From("com/test/1.0/my-package.jar.sha256"),
            sha256.getBytes(StandardCharsets.UTF_8)
        );
        final String sha512 = "cf713dd3f077719375e646a23dee1375725652f5f275b0bf25d326062b3a64535575acde6d27b547fcd735c870cf94badc4b2215aba9c3af5085567b4561ac28";
        bsto.save(
            new Key.From("com/test/1.0/my-package.jar.sha512"),
            sha512.getBytes(StandardCharsets.UTF_8)
        );
        final String mdfive = "dc829bf0d79e690c59cee708b527e6b7";
        bsto.save(
            new Key.From("com/test/1.0/my-package.jar.md5"),
            mdfive.getBytes(StandardCharsets.UTF_8)
        );
        MatcherAssert.assertThat(
            new RepositoryChecksums(storage).checksums(artifact).toCompletableFuture().get(),
            Matchers.allOf(
                Matchers.hasEntry("sha1", sha1),
                Matchers.hasEntry("sha256", sha256),
                Matchers.hasEntry("sha512", sha512),
                Matchers.hasEntry("md5", mdfive)
            )
        );
    }

    @Test
    void generatesChecksums() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("my-artifact.jar");
        final byte[] content = "my artifact content".getBytes();
        storage.save(key, new Content.From(content));
        new RepositoryChecksums(storage).generate(key).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Generates sha1",
                storage.value(new Key.From(String.format("%s.sha1", key.string()))).join().asString(),
            new IsEqual<>(DigestUtils.sha1Hex(content))
        );
        MatcherAssert.assertThat(
            "Generates sha256",
                storage.value(new Key.From(String.format("%s.sha256", key.string()))).join().asString(),
            new IsEqual<>(DigestUtils.sha256Hex(content))
        );
        MatcherAssert.assertThat(
            "Generates sha512",
                storage.value(new Key.From(String.format("%s.sha512", key.string()))).join().asString(),
            new IsEqual<>(DigestUtils.sha512Hex(content))
        );
        MatcherAssert.assertThat(
            "Generates md5",
                storage.value(new Key.From(String.format("%s.md5", key.string()))).join().asString(),
            new IsEqual<>(DigestUtils.md5Hex(content))
        );
    }
}
