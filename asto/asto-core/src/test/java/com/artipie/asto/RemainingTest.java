/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto;

import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Remaining}.
 * @since 0.32
 */
public final class RemainingTest {

    @Test
    public void readTwiceWithRestoreStrategy() throws Exception {
        final ByteBuffer buf = ByteBuffer.allocate(32);
        final byte[] array = new byte[]{1, 2, 3, 4};
        buf.put(array);
        buf.flip();
        MatcherAssert.assertThat(
            new Remaining(buf, true).bytes(), new IsEqual<>(array)
        );
        MatcherAssert.assertThat(
            new Remaining(buf, true).bytes(), new IsEqual<>(array)
        );
    }
}
