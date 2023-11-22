/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.misc;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.TestRpm;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Collections;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmByDigestCopy}.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("unchecked")
class RpmByDigestCopyTest {

    /**
     * Storage to copy from.
     */
    private Storage from;

    /**
     * Destination storage.
     */
    private Storage dest;

    @BeforeEach
    void init() {
        this.from = new InMemoryStorage();
        this.dest = new InMemoryStorage();
    }

    @Test
    void filtersFilesByDigests() throws IOException {
        final TestRpm rpm = new TestRpm.Abc();
        new TestRpm.Multiple(rpm, new TestRpm.Libdeflt()).put(this.from);
        new RpmByDigestCopy(
            this.from, Key.ROOT,
            new ListOf<String>("47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462")
        ).copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.list(Key.ROOT).join(),
            Matchers.containsInAnyOrder(
                new Key.From(rpm.path().getFileName().toString())
            )
        );
    }

    @Test
    void filtersFilesByExtension() throws IOException {
        final TestRpm rpm = new TestRpm.Abc();
        rpm.put(this.from);
        this.from.save(new Key.From("some/content"), new Content.From(Flowable.empty())).join();
        new RpmByDigestCopy(this.from, Key.ROOT, Collections.emptyList())
            .copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.list(Key.ROOT).join(),
            Matchers.containsInAnyOrder(
                new Key.From(rpm.path().getFileName().toString())
            )
        );
    }

    @Test
    void copiesAllWhenDigestsAreEmpty() throws IOException {
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Libdeflt()).put(this.from);
        new RpmByDigestCopy(this.from, Key.ROOT, Collections.emptyList())
            .copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.list(Key.ROOT).join(),
            Matchers.containsInAnyOrder(
                new Key.From(new TestRpm.Abc().path().getFileName().toString()),
                new Key.From(new TestRpm.Libdeflt().path().getFileName().toString())
            )
        );
    }

}
