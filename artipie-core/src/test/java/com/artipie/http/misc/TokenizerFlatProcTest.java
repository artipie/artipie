/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import com.artipie.asto.Remaining;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TokenizerFlatProc}.
 * @since 1.0
 */
final class TokenizerFlatProcTest {

    @Test
    void splitByDelimiter() {
        final Flowable<ByteBuffer> src = Flowable.fromArray(
            "hello ", "with ", "a ", "space\n ",
            "multi-line ", "strings\nand\n\nsome",
            " \nspaces ", "in ", "the ", "end ", " ", " "
        ).map(str -> ByteBuffer.wrap(str.getBytes()));
        final TokenizerFlatProc target = new TokenizerFlatProc("\n");
        src.subscribe(target);
        final List<String> split = Flowable.fromPublisher(target)
            .map(buf -> new String(new Remaining(buf).bytes())).toList().blockingGet();
        MatcherAssert.assertThat(
            split,
            Matchers.contains(
                "hello with a space",
                " multi-line strings",
                "and",
                "",
                "some ",
                "spaces in the end   "
            )
        );
    }
}
