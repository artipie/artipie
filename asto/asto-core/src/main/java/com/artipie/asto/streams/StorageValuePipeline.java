/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.ByteArray;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOSupplier;
import com.artipie.asto.misc.UncheckedRunnable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Processes storage value content as optional input stream and
 * saves the result back as output stream.
 *
 * @param <R> Result type
 * @since 1.5
 */
@SuppressWarnings("PMD.CognitiveComplexity")
public final class StorageValuePipeline<R> {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Storage item key to read from.
     */
    private final Key read;

    /**
     * Storage item key to write to.
     */
    private final Key write;

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param read Storage item key to read from
     * @param write Storage item key to write to
     */
    public StorageValuePipeline(final Storage asto, final Key read, final Key write) {
        this.asto = asto;
        this.read = read;
        this.write = write;
    }

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param key Item key
     */
    public StorageValuePipeline(final Storage asto, final Key key) {
        this(asto, key, key);
    }

    /**
     * Process storage item and save it back.
     *
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<Void> process(
        final BiConsumer<Optional<InputStream>, OutputStream> action
    ) {
        return this.processWithResult(
            (opt, input) -> {
                action.accept(opt, input);
                return null;
            }
        ).thenAccept(
            nothing -> {
            }
        );
    }

    /**
     * Process storage item, save it back and return some result.
     * Note that `action` must be called in async to avoid deadlock on input stream.
     * Also note that `PublishingOutputStream` currently needs `onComplete()` or `close()` call for reliable notifications.
     *
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action with the result
     * @throws ArtipieIOException On Error
     * @checkstyle ExecutableStatementCountCheck (100 lines)
     */
    public CompletionStage<R> processWithResult(
        final BiFunction<Optional<InputStream>, OutputStream, R> action
    ) {
        final AtomicReference<R> res = new AtomicReference<>();
        return this.asto.exists(this.read)
            .thenCompose(
                exists -> {
                    final CompletionStage<Optional<InputStream>> stage;
                    if (exists) {
                        stage = this.asto.value(this.read)
                            .thenApply(
                                content -> Optional.of(
                                    new ContentAsInputStream(content)
                                        .inputStream()
                                )
                            );
                    } else {
                        stage = CompletableFuture.completedFuture(Optional.empty());
                    }
                    return stage;
                }
            )
            .thenCompose(
                optional -> {
                    final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    final PublishingOutputStream output = new PublishingOutputStream(Schedulers.from(executor));
                    return CompletableFuture.runAsync(
                        () -> res.set(action.apply(optional, output)), executor
                    ).thenCompose(
                        unused -> {
                            final CompletableFuture<Void> saved = this.asto.save(this.write, new Content.From(output.publisher()));
                            output.setComplete();
                            return saved;
                        }
                    ).handle(
                        (unused, throwable) -> {
                            Throwable last = throwable;
                            try {
                                if (optional.isPresent()) {
                                    optional.get().close();
                                }
                            } catch (final IOException ex) {
                                if (last != null) {
                                    ex.addSuppressed(last);
                                }
                                last = ex;
                            }
                            try {
                                output.close();
                            } catch (final IOException ex) {
                                if (last != null) {
                                    ex.addSuppressed(last);
                                }
                                last = ex;
                            }
                            if (last != null) {
                                throw new ArtipieIOException(last);
                            }
                            return res.get();
                        });
                }
            );
    }

    /**
     * Represents {@link Content} as {@link InputStream}.
     * <p/>
     * This class is a {@link Subscriber}, that subscribes to the {@link Content}.
     * Subscription actions are performed on a {@link Scheduler} that is passed to the constructor.
     * Content data are written to the {@code channel}, that {@code channel} be constructed to write
     * bytes to the {@link PipedOutputStream} connected with {@link PipedInputStream},
     * the last stream is the resulting {@link InputStream}. When publisher complete,
     * {@link PipedOutputStream} is closed.
     *
     * @since 1.12
     */
    static class ContentAsInputStream extends DefaultSubscriber<ByteBuffer> {
        /**
         * Content.
         */
        private final Content content;

        /**
         * Scheduler to perform subscription actions on.
         */
        private final Scheduler scheduler;

        /**
         * {@code PipedOutputStream} to which bytes are to be written from {@code channel}.
         */
        private final PipedOutputStream out;

        /**
         * {@code PipedInputStream} connected to {@link #out},
         * it's used as the result {@code InputStream}.
         */
        private final PipedInputStream input;

        /**
         * {@code Channel} to write bytes from the {@link #content} to {@link #out}.
         */
        private final WritableByteChannel channel;

        /**
         * Ctor.
         *
         * @param content Content.
         */
        ContentAsInputStream(final Content content) {
            this(content, Schedulers.io());
        }

        /**
         * Ctor.
         *
         * @param content Content.
         * @param scheduler Scheduler to perform subscription actions on.
         */
        ContentAsInputStream(final Content content, final Scheduler scheduler) {
            this.content = content;
            this.scheduler = scheduler;
            this.out = new PipedOutputStream();
            this.input = new UncheckedIOSupplier<>(
                () -> new PipedInputStream(this.out)
            ).get();
            this.channel = Channels.newChannel(this.out);
        }

        @Override
        public void onNext(final ByteBuffer buffer) {
            Objects.requireNonNull(buffer);
            UncheckedRunnable.newIoRunnable(
                () -> {
                    while (buffer.hasRemaining()) {
                        this.channel.write(buffer);
                    }
                }
            ).run();
        }

        @Override
        public void onError(final Throwable err) {
            UncheckedRunnable.newIoRunnable(this.input::close).run();
        }

        @Override
        public void onComplete() {
            UncheckedRunnable.newIoRunnable(this.out::close).run();
        }

        /**
         * {@code Content} as {@code InputStream}.
         *
         * @return InputStream.
         */
        InputStream inputStream() {
            Flowable.fromPublisher(this.content)
                .subscribeOn(this.scheduler)
                .subscribe(this);
            return this.input;
        }
    }

    /**
     * Transfers {@link OutputStream} to {@code Publisher<ByteBuffer>}.
     * <p/>
     * Written to {@link OutputStream} bytes are accumulated in the buffer.
     * Buffer collects bytes during the period of time in ms or until the
     * number of bytes achieve a defined size. Then the buffer emits bytes
     * to the resulting publisher.
     *
     * @since 1.12
     */
    static class PublishingOutputStream extends OutputStream {
        /**
         * Default period of time buffer collects bytes before it is emitted to publisher (ms).
         */
        private static final long DEFAULT_TIMESPAN = 100L;

        /**
         * Default maximum size of each buffer before it is emitted.
         */
        private static final int DEFAULT_BUF_SIZE = 4 * 1024;

        /**
         * Resulting publisher.
         */
        private final UnicastProcessor<ByteBuffer> pub;

        /**
         * Buffer processor to collect bytes.
         */
        private final UnicastProcessor<Byte> bufproc;

        /**
         * Ctor.
         *
         *  @param scheduler Target rx scheduler for execution.
         */
        PublishingOutputStream(final Scheduler scheduler) {
            this(
                PublishingOutputStream.DEFAULT_TIMESPAN,
                TimeUnit.MILLISECONDS,
                PublishingOutputStream.DEFAULT_BUF_SIZE,
                scheduler
            );
        }

        /**
         * Ctor.
         */
        PublishingOutputStream() {
            this(
                PublishingOutputStream.DEFAULT_TIMESPAN,
                TimeUnit.MILLISECONDS,
                PublishingOutputStream.DEFAULT_BUF_SIZE,
                Schedulers.io()
            );
        }

        /**
         * Ctor.
         *
         * @param timespan The period of time buffer collects bytes
         *  before it is emitted to publisher.
         * @param unit The unit of time which applies to the timespan argument.
         * @param count The maximum size of each buffer before it is emitted.
         * @param scheduler Target rx scheduler for execution.
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        PublishingOutputStream(
            final long timespan,
            final TimeUnit unit,
            final int count,
            final Scheduler scheduler
        ) {
            this.pub = UnicastProcessor.create();
            this.bufproc = UnicastProcessor.create();
            this.bufproc.buffer(timespan, unit, count)
                .doOnNext(
                    list -> this.pub.onNext(
                        ByteBuffer.wrap(new ByteArray(list).primitiveBytes())
                    )
                )
                .subscribeOn(scheduler)
                .doOnComplete(this.pub::onComplete)
                .subscribe();
        }

        @Override
        public void write(final int b) throws IOException {
            this.bufproc.onNext((byte) b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.bufproc.onComplete();
        }

        public void setComplete() {
            this.bufproc.onComplete();
        }

        /**
         * Resulting publisher.
         *
         * @return Publisher.
         */
        Publisher<ByteBuffer> publisher() {
            return this.pub;
        }
    }
}
