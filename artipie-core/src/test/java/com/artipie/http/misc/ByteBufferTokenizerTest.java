/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for ByteBufferTokenizer.
 *
 * @since 1.0
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
final class ByteBufferTokenizerTest {

    @ParameterizedTest
    @CsvSource({
        "/,---test123,on/e---test123two---test123t/hree---test123four,one/two/three/four",
        "/,--any--,one,one",
        "/,|,|one|two|,/one/two/",
        "/,|,o/n/e/|/t/w/o/|/t/h/r/e/e,one/two/three",
        "/,|,|||,///",
        "/,--boundary--,one-/-/b/o/u/n/d/a/r/y/-/-two--b/oun/dar/y--three,one/two/three",
        "/,---,o/n/e/---/t/w/o--/-three---four-/-/-,one/two/three/four/"
    })
    public void splitByTokens(final String split, final String delim, final String source,
        final String expect) {
        for (int cap = 1; cap < source.length() + 1; ++cap) {
            final Deque<ByteBuffer> result = new LinkedList<>();
            try (AccReceiver rec = new AccReceiver(result);
                ByteBufferTokenizer target = new ByteBufferTokenizer(rec, delim.getBytes(), cap)) {
                Arrays.asList(source.split(split)).stream()
                    .map(String::getBytes)
                    .map(ByteBuffer::wrap)
                    .map(ByteBuffer::asReadOnlyBuffer)
                    .forEach(target::push);
            }
            MatcherAssert.assertThat(
                result.stream().map(ByteBufferTokenizerTest::bufToStr).collect(Collectors.toList()),
                Matchers.contains(expect.split(split, -1))
            );
        }
    }

    @Test
    void deliverEmptyPart() {
        final Deque<ByteBuffer> result = new LinkedList<>();
        try (AccReceiver rec = new AccReceiver(result);
            ByteBufferTokenizer tokenizer = new ByteBufferTokenizer(rec, "|".getBytes(), 1)) {
            tokenizer.push(
                ByteBuffer.wrap("|second||fourth".getBytes(StandardCharsets.US_ASCII))
            );
        }
        MatcherAssert.assertThat(
            result.stream().map(ByteBufferTokenizerTest::bufToStr).collect(Collectors.toList()),
            Matchers.contains("", "second", "", "fourth")
        );
    }

    private static String bufToStr(final ByteBuffer buf) {
        final byte[] bts = new byte[buf.remaining()];
        buf.get(bts);
        return new String(bts);
    }

    /**
     * Accummulating receiver for test.
     * It accumulates tokens in queue provided.
     * @since 1.0
     */
    private static final class AccReceiver implements ByteBufferTokenizer.Receiver, Closeable {

        /**
         * Result queue.
         */
        private final Deque<ByteBuffer> result;

        /**
         * Chunk processor.
         */
        private final Function<ByteBuffer, ByteBuffer> processor;

        /**
         * New test receiver.
         * @param result Queue
         */
        AccReceiver(final Deque<ByteBuffer> result) {
            this(result, Function.identity());
        }

        /**
         * New test receiver with processor.
         * @param result Queue
         * @param processor Chunk processor
         */
        AccReceiver(final Deque<ByteBuffer> result,
            final Function<ByteBuffer, ByteBuffer> processor) {
            this.result = result;
            this.processor = processor;
        }

        @Override
        public void receive(final ByteBuffer next, final boolean end) {
            if (this.result.isEmpty()) {
                this.result.addFirst(next);
            } else {
                final ByteBuffer last = this.result.getLast();
                final ByteBuffer concat = ByteBuffer.allocate(last.remaining() + next.remaining());
                concat.put(last);
                concat.put(next);
                concat.flip();
                this.result.removeLast();
                this.result.addLast(concat);
            }
            if (end) {
                this.result.addLast(ByteBuffer.allocate(0));
            }
        }

        @Override
        public void close() {
            this.result.removeLast();
        }
    }
}
