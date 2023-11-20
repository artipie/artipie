/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rq.multipart;

import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import wtf.g4s8.mime.MimeType;

/**
 * Multipart request.
 * <p>
 * Parses multipart request into body parts as publisher of {@code Request}s.
 * It accepts bodies acoording to
 * <a href="https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html">RFC-1341-7.2</a>
 * specification.
 * </p>
 *
 * @implNote Since the multipart body is always received sequentially part by part,
 * the parts() method does not publish the next part until the previous is fully read.
 * @implNote The implementation does not keep request part data in memory or storage,
 * it should process each chunk and send to proper downstream.
 * @implNote The body part will not be parsed until {@code parts()} method call.
 * @since 1.0
 */
public final class RqMultipart {

    /**
     * Content type.
     */
    private ContentType ctype;

    /**
     * Body upstream.
     */
    private Publisher<ByteBuffer> upstream;

    /**
     * Multipart request from headers and body upstream.
     * @param headers Request headers
     * @param body Upstream
     */
    public RqMultipart(final Headers headers, final Publisher<ByteBuffer> body) {
        this(new ContentType(headers), body);
    }

    /**
     * Multipart request from content type and body upstream.
     *
     * @param ctype Content type
     * @param body Upstream
     */
    public RqMultipart(final ContentType ctype, final Publisher<ByteBuffer> body) {
        this.ctype = ctype;
        this.upstream = body;
    }

    /**
     * Body parts.
     *
     * @return Publisher of parts
     */
    public Publisher<Part> parts() {
        final MultiParts pub = new MultiParts(this.boundary());
        pub.subscribeAsync(this.upstream);
        return pub;
    }

    /**
     * Inspect all parts of multipart request.
     * <p>
     * This method uses {@code Inspector} function as parameter to
     * construct result publisher. Inspector receives all parts one by one
     * with sink parameter for downstream. Inspector MUST exlplicitly accept or
     * ignore each part, in case if some part is not accepter or ignored,
     * publisher WILL fail with exception. All accepted parts will be sent to
     * downstream publisher one by one depends on downstream processing logic.
     * In case if inspector replaces the body of some part on accept or ignore calls,
     * it MUST ensure that origin part publisher will be read either directly in
     * insepct method or later with replaced publisher (e.g. valid replacement could
     * be a {@code map()} function called on origin publisher).
     * </p>
     * @param inspector Function to inspect the part
     * @return Accepted parts by inspector
     */
    public Publisher<? extends Part> inspect(final Inspector inspector) {
        return Flowable.fromPublisher(this.parts()).flatMapSingle(
            part -> {
                final InternalSink sink = new InternalSink();
                return Completable.fromFuture(inspector.inspect(part, sink).toCompletableFuture())
                    .andThen(sink.filter());
            }
        ).filter(part -> part != Part.EMPTY);
    }

    /**
     * Filter parts by headers predicate.
     * @param pred Headers predicate
     * @return Parts publisher
     * @deprecated Use inspect method directly, see #418 ticket for details
     */
    @Deprecated
    public Publisher<? extends Part> filter(final Predicate<Headers> pred) {
        return this.inspect(
            (part, sink) -> {
                if (pred.test(part.headers())) {
                    sink.accept(part);
                } else {
                    sink.ignore(part);
                }
                final CompletableFuture<Void> res = new CompletableFuture<>();
                res.complete(null);
                return res;
            }
        );
    }

    /**
     * Multipart boundary.
     * @return Boundary string
     */
    private String boundary() {
        final String header = MimeType.of(this.ctype.getValue()).param("boundary").orElseThrow(
            () -> new ArtipieHttpException(
                RsStatus.BAD_REQUEST,
                "Content-type boundary param missed"
            )
        );
        return String.format("\r\n--%s", header);
    }

    /**
     * Part of multipart.
     *
     * @since 1.0
     */
    public interface Part extends Publisher<ByteBuffer> {

        /**
         * Empty part.
         */
        Part EMPTY = new EmptyPart(Flowable.never());

        /**
         * Part headers.
         *
         * @return Headers
         */
        Headers headers();
    }

    /**
     * A function which inspects upstream parts.
     *
     * @implNote it MUST either
     * accept or ignore each part using sink downstream. In case if
     * some part is not accepted or ignored, the publisher WILL fail
     * with exception.
     * @implNote Sink parameter is not thread safe - do not try to
     * update its state asynchronously, update should be finished
     * before result future completes.
     * @since 1.1
     */
    @FunctionalInterface
    public interface Inspector {

        /**
         * Inspect a part and report the result to sink.
         * @param part Upstream part
         * @param sink Downstream sink
         * @return Future on complete
         */
        CompletionStage<Void> inspect(Part part, Sink sink);
    }

    /**
     * Inspection sink.
     * <p>
     * Provides the methods for accepting or ignoring upstream items.
     * </p>
     * @since 1.1
     */
    public interface Sink {

        /**
         * Accept item for downstream.
         * @param part Part for downstream
         */
        void accept(Part part);

        /**
         * Ignore item.
         * @param part Part will be drained and ignored
         */
        void ignore(Part part);
    }

    /**
     * Internal sink implementation to keep parts in memory.
     * @since 1.1
     */
    private static final class InternalSink implements Sink {

        /**
         * Accepted item.
         */
        private Part accepted;

        /**
         * Ignored item.
         */
        private Part ignored;

        @Override
        public void accept(final Part part) {
            this.check();
            this.accepted = part;
        }

        @Override
        public void ignore(final Part part) {
            this.check();
            this.ignored = part;
        }

        /**
         * Create filter single source which either returns accepted item, or
         * drain ignored item and return empty after that.
         * @return Single source
         * @checkstyle ReturnCountCheck (20 lines)
         */
        @SuppressWarnings({"PMD.ConfusingTernary", "PMD.OnlyOneReturn"})
        Single<? extends Part> filter() {
            if (this.accepted != null) {
                return Single.just(this.accepted);
            } else if (this.ignored != null) {
                return Flowable.fromPublisher(this.ignored)
                    .ignoreElements().toSingleDefault(Part.EMPTY);
            } else {
                return Single.error(
                    () -> new IllegalStateException(
                        "Part should be accepted or ignored explicitly"
                    )
                );
            }
        }

        /**
         * Check if part was accepted or rejected.
         * @param err
         */
        private void check() {
            if (this.accepted != null) {
                throw new IllegalStateException("Part was accepted already");
            }
            if (this.ignored != null) {
                throw new IllegalStateException("Part was ignored already");
            }
        }
    }
}
