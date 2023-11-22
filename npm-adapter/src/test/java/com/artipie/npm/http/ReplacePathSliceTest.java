/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests ReplacePathSlice.
 * @since 0.6
 */
@ExtendWith(MockitoExtension.class)
public class ReplacePathSliceTest {

    /**
     * Underlying slice mock.
     */
    @Mock
    private Slice underlying;

    @Test
    public void rootPathWorks() {
        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice("/", this.underlying);
        final String expected = "GET /some-path HTTP/1.1\r\n";
        slice.response(expected, Collections.emptyList(), sub -> ByteBuffer.allocate(0));
        MatcherAssert.assertThat(
            path.getValue(),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void compoundPathWorks() {
        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice(
            "/compound/ctx/path",
            this.underlying
        );
        slice.response(
            "GET /compound/ctx/path/abc-def HTTP/1.1\r\n",
            Collections.emptyList(),
            sub -> ByteBuffer.allocate(0)
        );
        MatcherAssert.assertThat(
            path.getValue(),
            new IsEqual<>("GET /abc-def HTTP/1.1\r\n")
        );
    }
}
