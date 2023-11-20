/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import io.reactivex.internal.functions.Functions;
import io.reactivex.subjects.SingleSubject;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Test case for {@link MultiPart}.
 *
 * @since 1.0
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.TestClassWithoutTestCases", "PMD.OnlyOneReturn",
        "PMD.JUnit4TestShouldUseBeforeAnnotation"
    }
)
public final class MultiPartTckTest extends PublisherVerification<ByteBuffer> {

    /**
     * Test buffer size.
     */
    private static final int TEST_BUF = 4096;

    /**
     * Ctor.
     */
    public MultiPartTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(final long size) {
        final SingleSubject<RqMultipart.Part> subj = SingleSubject.create();
        final MultiPart part = new MultiPart(
            Completion.FAKE, subj::onSuccess,
            Executors.newCachedThreadPool()
        );
        Executors.newCachedThreadPool().submit(
            () -> {
                part.push(ByteBuffer.wrap("\r\n\r\n".getBytes()));
                final byte[] data = new byte[MultiPartTckTest.TEST_BUF];
                Arrays.fill(data, (byte) 'A');
                for (long pos = size; pos > 0; --pos) {
                    part.push(ByteBuffer.wrap(data));
                }
                part.flush();
            }
        );
        return subj.flatMapPublisher(Functions.identity());
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return 1;
    }
}
