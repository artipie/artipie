/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoChecksumAndName}.
 * @since 1.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoChecksumAndNameTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void calculatesChecksumsByRootKey() {
        final String first = "first.rpm";
        final String second = "second.rpm";
        this.asto.save(new Key.From(first), new Content.From(first.getBytes())).join();
        this.asto.save(new Key.From("abc", second), new Content.From(second.getBytes())).join();
        this.asto.save(new Key.From("any"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new AstoChecksumAndName(this.asto, Digest.SHA256).calculate(Key.ROOT)
                .toCompletableFuture().join().entrySet(),
            Matchers.hasItems(
                new MapEntry<>("first.rpm", DigestUtils.sha256Hex(first.getBytes())),
                new MapEntry<>("abc/second.rpm", DigestUtils.sha256Hex(second.getBytes()))
            )
        );
    }

    @Test
    void calculatesChecksums() {
        final Key init = new Key.From("init");
        final String abc = "abc.rpm";
        final String xyz = "xyz.rpm";
        this.asto.save(new Key.From(init, abc), new Content.From(abc.getBytes())).join();
        this.asto.save(new Key.From(init, "sub", xyz), new Content.From(xyz.getBytes())).join();
        this.asto.save(new Key.From("item"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new AstoChecksumAndName(this.asto, Digest.SHA256).calculate(init)
                .toCompletableFuture().join().entrySet(),
            Matchers.hasItems(
                new MapEntry<>("abc.rpm", DigestUtils.sha256Hex(abc.getBytes())),
                new MapEntry<>("sub/xyz.rpm", DigestUtils.sha256Hex(xyz.getBytes()))
            )
        );
    }

}
