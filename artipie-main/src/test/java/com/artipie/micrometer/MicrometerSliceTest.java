/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.Slice;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.slice.SliceSimple;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Test for {@link MicrometerSlice}.
 */
class MicrometerSliceTest {

    private SimpleMeterRegistry registry;

    @BeforeEach
    void init() {
        this.registry = new SimpleMeterRegistry();
    }

    @Test
    void addsSummaryToRegistry() {
        final String path = "/same/path";
        assertResponse(
            ResponseBuilder.ok().body(Flowable.fromArray(
                ByteBuffer.wrap("Hello ".getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap("world!".getBytes(StandardCharsets.UTF_8))
            )).build(),
            new RequestLine(RqMethod.GET, path),
            RsStatus.OK
        );
        assertResponse(
            ResponseBuilder.ok().body("abc".getBytes(StandardCharsets.UTF_8)).build(),
            new RequestLine(RqMethod.GET, path),
            RsStatus.OK
        );
        assertResponse(
            ResponseBuilder.from(RsStatus.CONTINUE).build(),
            new RequestLine(RqMethod.POST, "/a/b/c"),
            RsStatus.CONTINUE
        );
        String actual = registry.getMetersAsString();

        List.of(
            Matchers.containsString("artipie.request.body.size(DISTRIBUTION_SUMMARY)[method='POST']; count=0.0, total=0.0 bytes, max=0.0 bytes"),
            Matchers.containsString("artipie.request.body.size(DISTRIBUTION_SUMMARY)[method='GET']; count=0.0, total=0.0 bytes, max=0.0 bytes"),
            Matchers.containsString("artipie.request.counter(COUNTER)[method='POST', status='CONTINUE']; count=1.0"),
            Matchers.containsString("artipie.request.counter(COUNTER)[method='GET', status='OK']; count=2.0"),
            Matchers.containsString("artipie.response.body.size(DISTRIBUTION_SUMMARY)[method='POST']; count=0.0, total=0.0 bytes, max=0.0 bytes"),
            Matchers.containsString("artipie.response.body.size(DISTRIBUTION_SUMMARY)[method='GET']; count=3.0, total=15.0 bytes, max=6.0 bytes"),
            Matchers.containsString("artipie.slice.response(TIMER)[status='OK']; count=2.0, total_time"),
            Matchers.containsString("artipie.slice.response(TIMER)[status='CONTINUE']; count=1.0, total_time")
        ).forEach(m -> MatcherAssert.assertThat(actual, m));
    }

    private void assertResponse(Response res, RequestLine line, RsStatus expected) {
        Slice slice = new MicrometerSlice(new SliceSimple(res), this.registry);
        Response actual = slice.response(line, Headers.EMPTY, Content.EMPTY).join();
        ResponseAssert.check(actual, expected);
        actual.body().asString();
    }
}
