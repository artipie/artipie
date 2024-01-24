/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.debian.metadata.InRelease;
import com.artipie.debian.metadata.Release;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ReleaseSlice}.
 * @since 0.2
 */
class ReleaseSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void createsReleaseFileAndForwardsResponse() {
        final FakeRelease release = new FakeRelease(new Key.From("any"));
        final FakeInRelease inrelease = new FakeInRelease();
        MatcherAssert.assertThat(
            "Response is CREATED",
            new ReleaseSlice(
                new SliceSimple(new RsWithStatus(RsStatus.CREATED)),
                this.asto,
                release,
                inrelease
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.GET, "/any/request/line")
            )
        );
        MatcherAssert.assertThat(
            "Release file was created",
            release.count.get(),
            new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "InRelease file was created",
            inrelease.count.get(),
            new IsEqual<>(1)
        );
    }

    @Test
    void doesNothingAndForwardsResponse() {
        final Key key = new Key.From("dists/my-repo/Release");
        this.asto.save(key, Content.EMPTY).join();
        final FakeRelease release = new FakeRelease(key);
        final FakeInRelease inrelease = new FakeInRelease();
        MatcherAssert.assertThat(
            "Response is OK",
            new ReleaseSlice(
                new SliceSimple(new RsWithStatus(RsStatus.OK)),
                this.asto,
                release,
                inrelease
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/not/important")
            )
        );
        MatcherAssert.assertThat(
            "Release file was not created",
            release.count.get(),
            new IsEqual<>(0)
        );
        MatcherAssert.assertThat(
            "InRelease file was not created",
            inrelease.count.get(),
            new IsEqual<>(0)
        );
    }

    /**
     * Fake {@link Release} implementation for the test.
     * @since 0.2
     */
    private static final class FakeRelease implements Release {

        /**
         * Method calls count.
         */
        private final AtomicInteger count;

        /**
         * Release file key.
         */
        private final Key rfk;

        /**
         * Ctor.
         * @param key Release file key
         */
        private FakeRelease(final Key key) {
            this.rfk = key;
            this.count = new AtomicInteger(0);
        }

        @Override
        public CompletionStage<Void> create() {
            this.count.incrementAndGet();
            return CompletableFuture.allOf();
        }

        @Override
        public CompletionStage<Void> update(final Key pckg) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public Key key() {
            return this.rfk;
        }

        @Override
        public Key gpgSignatureKey() {
            throw new NotImplementedException("Not implemented yet");
        }
    }

    /**
     * Fake implementation of {@link InRelease}.
     * @since 0.4
     */
    private static final class FakeInRelease implements InRelease {

        /**
         * Method calls count.
         */
        private final AtomicInteger count;

        /**
         * Ctor.
         */
        private FakeInRelease() {
            this.count = new AtomicInteger(0);
        }

        @Override
        public CompletionStage<Void> generate(final Key release) {
            this.count.incrementAndGet();
            return CompletableFuture.allOf();
        }

        @Override
        public Key key() {
            return null;
        }
    }

}
