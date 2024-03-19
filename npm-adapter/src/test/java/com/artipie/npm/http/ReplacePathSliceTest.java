/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests ReplacePathSlice.
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
        final ArgumentCaptor<RequestLine> path = ArgumentCaptor.forClass(RequestLine.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice("/", this.underlying);
        final RequestLine expected = RequestLine.from("GET /some-path HTTP/1.1");
        slice.response(expected, Headers.EMPTY, Content.EMPTY);
        Assertions.assertEquals(expected, path.getValue());
    }

    @Test
    public void compoundPathWorks() {
        final ArgumentCaptor<RequestLine> path = ArgumentCaptor.forClass(RequestLine.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice(
            "/compound/ctx/path",
            this.underlying
        );
        slice.response(
            RequestLine.from("GET /compound/ctx/path/abc-def HTTP/1.1"),
            Headers.EMPTY,
            Content.EMPTY
        );
        Assertions.assertEquals(new RequestLine("GET", "/abc-def"), path.getValue());
    }
}
