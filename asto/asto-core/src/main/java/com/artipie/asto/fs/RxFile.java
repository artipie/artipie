/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.asto.ArtipieIOException;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subjects.SingleSubject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cqfn.rio.file.File;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * <p>
 * The implementation is based on {@link org.cqfn.rio.file.File} from
 * <a href="https://github.com/cqfn/rio">cqfn/rio</a>.
 *
 * @since 0.12
 */
public class RxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * Thread pool.
     */
    private final ExecutorService exec;

    /**
     * Ctor.
     * @param file The wrapped file
     */
    public RxFile(final Path file) {
        this.file = file;
        this.exec = Executors.newCachedThreadPool();
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<ByteBuffer> flow() {
        return Flowable.fromPublisher(new File(this.file).content());
    }

    /**
     * Save a flow of bytes to a file.
     *
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<ByteBuffer> flow) {
        return Completable.defer(
            () -> CompletableInterop.fromFuture(
                new File(this.file).write(
                    flow,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
            )
        );
    }

    /**
     * Move file to new location.
     *
     * @param target Target path the file is moved to.
     * @return Completion or error signal
     */
    public Completable move(final Path target) {
        return Completable.defer(
            () -> {
                final CompletableSubject res = CompletableSubject.create();
                this.exec.submit(
                    () -> {
                        try {
                            Files.move(this.file, target, StandardCopyOption.REPLACE_EXISTING);
                            res.onComplete();
                        } catch (final IOException iex) {
                            res.onError(new ArtipieIOException(iex));
                        }
                    }
                );
                return res;
            }
        );
    }

    /**
     * Delete file.
     *
     * @return Completion or error signal
     */
    public Completable delete() {
        return Completable.defer(
            () -> {
                final CompletableSubject res = CompletableSubject.create();
                this.exec.submit(
                    () -> {
                        try {
                            Files.delete(this.file);
                            res.onComplete();
                        } catch (final IOException iex) {
                            res.onError(new ArtipieIOException(iex));
                        }
                    }
                );
                return res;
            }
        );
    }

    /**
     * Get file size.
     *
     * @return File size in bytes.
     */
    public Single<Long> size() {
        return Single.defer(
            () -> {
                final SingleSubject<Long> res = SingleSubject.create();
                this.exec.submit(
                    () -> {
                        try {
                            res.onSuccess(Files.size(this.file));
                        } catch (final IOException iex) {
                            res.onError(new ArtipieIOException(iex));
                        }
                    }
                );
                return res;
            }
        );
    }
}
