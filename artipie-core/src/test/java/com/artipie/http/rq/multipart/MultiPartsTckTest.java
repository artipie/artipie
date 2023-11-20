/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
public final class MultiPartsTckTest extends PublisherVerification<Integer> {

    /**
     * Test environment.
     */
    private final TestEnvironment env;

    /**
     * Primary ctor.
     * @param env Tets environemnt
     */
    public MultiPartsTckTest(final TestEnvironment env) {
        super(env);
        this.env = env;
    }

    @Override
    public Publisher<Integer> createPublisher(final long size) {
        final String boundary = "--bnd";
        final MultiParts target = new MultiParts(boundary);
        target.subscribeAsync(
            Flowable.rangeLong(0, size).map(id -> this.newChunk(id, id == size - 1, boundary))
        );
        return Flowable.fromPublisher(target).flatMapSingle(
            part -> Flowable.fromPublisher(part).reduce(0, (acc, buf) -> acc + buf.remaining())
        );
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 20;
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return 1;
    }

    /**
     * Create new chunk.
     * @param id Chunk number
     * @param end True if a last chank in sequence
     * @param boundary Boundary string
     * @return Chunk buffer
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private ByteBuffer newChunk(final long id, final boolean end, final String boundary) {
        final StringBuilder chunk = new StringBuilder(40);
        if (id == 0) {
            chunk.append("\r\n");
        }
        chunk.append(boundary).append(String.format("\r\nID: %d\r\n\r\n<id>%d</id>\r\n", id, id));
        if (end) {
            chunk.append(String.format("%s--", boundary)).append("\r\n");
        }
        this.env.debug(
            String.format(
                "newChunk(id=%d, env=%b, boundary='%s') -> %s",
                id, end, boundary, chunk.toString()
            )
        );
        return ByteBuffer.wrap(chunk.toString().getBytes(StandardCharsets.US_ASCII));
    }
}
