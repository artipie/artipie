/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.asto.ext.PublisherAs;
import io.reactivex.internal.functions.Functions;
import io.reactivex.subjects.SingleSubject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test case for {@link MultiPart}.
 *
 * @since 1.0
 */
final class MultiPartTest {

    /**
     * Multi part processor executor.
     */
    private ExecutorService exec;

    @BeforeEach
    void setUp() {
        this.exec = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.exec.shutdown();
        this.exec.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void parsePart() throws Exception {
        final SingleSubject<RqMultipart.Part> subj = SingleSubject.create();
        final MultiPart part = new MultiPart(Completion.FAKE, subj::onSuccess, this.exec);
        Executors.newCachedThreadPool().submit(
            () -> {
                for (final String chunk : Arrays.asList(
                    "Content-l", "ength", ": 24\r\n",
                    "Con", "tent-typ", "e: ", "appl", "ication/jso", "n\r\n\r\n{\"foo",
                    "\": \"b", "ar\", ", "\"val\": [4]}"
                )) {
                    part.push(ByteBuffer.wrap(chunk.getBytes(StandardCharsets.US_ASCII)));
                }
                part.flush();
            }
        );
        MatcherAssert.assertThat(
            new PublisherAs(subj.flatMapPublisher(Functions.identity()))
                .string(StandardCharsets.US_ASCII)
                .toCompletableFuture().get(),
            Matchers.equalTo("{\"foo\": \"bar\", \"val\": [4]}")
        );
    }

    @Test
    @Timeout(1)
    void parseEmptyBody() throws Exception {
        final SingleSubject<RqMultipart.Part> subj = SingleSubject.create();
        final MultiPart part = new MultiPart(Completion.FAKE, subj::onSuccess, this.exec);
        Executors.newCachedThreadPool().submit(
            () -> {
                part.push(
                    ByteBuffer.wrap(
                        "Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)
                    )
                );
                part.flush();
            }
        );
        MatcherAssert.assertThat(
            new PublisherAs(subj.flatMapPublisher(Functions.identity()))
                .string(StandardCharsets.US_ASCII)
                .toCompletableFuture().get(),
            Matchers.equalTo("")
        );
    }
}
