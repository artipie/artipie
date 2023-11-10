/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.http;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PushChartSlice#bufsToByteArr(List)}.
 *
 * @since 0.1
 */
public class BufsToByteArrTest {

    @Test
    public void copyIsCorrect() {
        final String actual = new String(
            PushChartSlice.bufsToByteArr(
                Arrays.asList(
                    ByteBuffer.wrap("123".getBytes()),
                    ByteBuffer.wrap("456".getBytes()),
                    ByteBuffer.wrap("789".getBytes())
                )
            )
        );
        MatcherAssert.assertThat(
            actual,
            new IsEqual<>("123456789")
        );
    }
}
