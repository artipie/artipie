/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.Digest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoChecksumAndSize}.
 * @since 1.9
 */
class AstoChecksumAndSizeTest {

    @Test
    void savesChecksumAndSize() {
        final Storage asto = new InMemoryStorage();
        final Charset charset = StandardCharsets.UTF_8;
        final String item = "storage_item";
        final byte[] bfirst = item.getBytes(charset);
        final BlockingStorage blsto = new BlockingStorage(asto);
        blsto.save(new Key.From(item), bfirst);
        final Digest dgst = Digest.SHA256;
        new AstoChecksumAndSize(asto, dgst).calculate(new Key.From(item))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new String(
                blsto.value(new Key.From(String.format("%s.%s", item, dgst.name()))), charset
            ),
            new IsEqual<>(String.format("%s %s", DigestUtils.sha256Hex(bfirst), bfirst.length))
        );
    }

}
