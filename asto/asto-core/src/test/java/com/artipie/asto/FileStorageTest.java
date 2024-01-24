/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link FileStorage}.
 *
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
final class FileStorageTest {

    /**
     * Test temp directory.
     */
    @TempDir
    Path tmp;

    /**
     * File storage used in tests.
     */
    private FileStorage storage;

    @BeforeEach
    void setUp() {
        this.storage = new FileStorage(this.tmp);
    }

    @Test
    void savesAndLoads() throws Exception {
        final byte[] content = "Hello world!!!".getBytes();
        final Key key = new Key.From("a", "b", "test.deb");
        this.storage.save(
            key,
            new Content.OneTime(new Content.From(content))
        ).get();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(this.storage.value(key).get()).single().blockingGet(),
                true
            ).bytes(),
            Matchers.equalTo(content)
        );
    }

    @Test
    void saveOverwrites() throws Exception {
        final byte[] original = "1".getBytes(StandardCharsets.UTF_8);
        final byte[] updated = "2".getBytes(StandardCharsets.UTF_8);
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key key = new Key.From("foo");
        blocking.save(key, original);
        blocking.save(key, updated);
        MatcherAssert.assertThat(
            blocking.value(key),
            new IsEqual<>(updated)
        );
    }

    @Test
    void saveBadContentDoesNotLeaveTrace() {
        this.storage.save(
            new Key.From("a/b/c/"),
            new Content.From(Flowable.error(new IllegalStateException()))
        ).exceptionally(ignored -> null).join();
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void shouldAlwaysSaveInStorageSandbox() {
        final Key key = new Key.From("../../etc/password");
        final Exception cex = Assertions.assertThrows(
            Exception.class,
            () -> this.storage.save(key, Content.EMPTY).get()
        );
        MatcherAssert.assertThat(
            "Should throw an io exception while saving",
            ExceptionUtils.getRootCause(cex).getClass(),
            new IsEqual<>(IOException.class)
        );
        MatcherAssert.assertThat(
            "Should throw with exception message while saving",
            ExceptionUtils.getRootCause(cex).getMessage(),
            new IsEqual<>(String.format("Entry path is out of storage: %s", key))
        );
    }

    @Test
    void shouldAlwaysDeleteInStorageSandbox() throws IOException {
        final Path myfolder = FileStorageTest.createNewDirectory(this.tmp, "my-folder");
        FileStorageTest.createNewFile(myfolder, "file.txt");
        final Path afolder = FileStorageTest.createNewDirectory(this.tmp, "another-folder");
        final FileStorage sto = new FileStorage(afolder);
        final Key key = new Key.From("../my-folder/file.txt");
        final Exception cex = Assertions.assertThrows(
            Exception.class,
            () -> sto.delete(key).get()
        );
        MatcherAssert.assertThat(
            "Should throw an io exception while deleting",
            ExceptionUtils.getRootCause(cex).getClass(),
            new IsEqual<>(IOException.class)
        );
        MatcherAssert.assertThat(
            "Should throw with exception message while deleting",
            ExceptionUtils.getRootCause(cex).getMessage(),
            new IsEqual<>(String.format("Entry path is out of storage: %s", key))
        );
    }

    @Test
    void shouldAlwaysMoveFromStorageSandbox() throws IOException {
        final Path myfolder =
            FileStorageTest.createNewDirectory(this.tmp, "my-folder-move-from");
        FileStorageTest.createNewFile(myfolder, "file.txt");
        final Path afolder =
            FileStorageTest.createNewDirectory(this.tmp, "another-folder-move-from");
        final FileStorage sto = new FileStorage(afolder);
        final Key source = new Key.From("../my-folder-move-from/file.txt");
        final Key destination = new Key.From("another-folder-move-from/file.txt");
        final Exception cex = Assertions.assertThrows(
            Exception.class,
            () -> sto.move(source, destination).get()
        );
        MatcherAssert.assertThat(
            "Should throw an io exception while moving from",
            ExceptionUtils.getRootCause(cex).getClass(),
            new IsEqual<>(IOException.class)
        );
        MatcherAssert.assertThat(
            "Should throw with exception message while moving from",
            ExceptionUtils.getRootCause(cex).getMessage(),
            new IsEqual<>(String.format("Entry path is out of storage: %s", source))
        );
    }

    @Test
    void shouldAlwaysMoveToStorageSandbox() throws IOException {
        final Path myfolder =
            FileStorageTest.createNewDirectory(this.tmp, "my-folder-move-to");
        FileStorageTest.createNewFile(myfolder, "file.txt");
        final Path afolder =
            FileStorageTest.createNewDirectory(this.tmp, "another-folder-move-to");
        FileStorageTest.createNewFile(afolder, "file.txt");
        final FileStorage sto = new FileStorage(afolder);
        final Key source = new Key.From("another-folder-move-to/file.txt");
        final Key destination = new Key.From("../my-folder-move-to/file.txt");
        final Exception cex = Assertions.assertThrows(
            Exception.class,
            () -> sto.move(source, destination).get()
        );
        MatcherAssert.assertThat(
            "Should throw an io exception while moving to",
            ExceptionUtils.getRootCause(cex).getClass(),
            new IsEqual<>(IOException.class)
        );
        MatcherAssert.assertThat(
            "Should throw with exception message while moving to",
            ExceptionUtils.getRootCause(cex).getMessage(),
            new IsEqual<>(String.format("Entry path is out of storage: %s", destination))
        );
    }

    @Test
    @SuppressWarnings("deprecation")
    void readsTheSize() throws Exception {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("withSize");
        bsto.save(key, new byte[]{0x00, 0x00, 0x00});
        MatcherAssert.assertThat(
            bsto.size(key),
            // @checkstyle MagicNumberCheck (1 line)
            Matchers.equalTo(3L)
        );
    }

    @Test
    void blockingWrapperWorks() throws Exception {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final String content = "hello, friend!";
        final Key key = new Key.From("t", "y", "testb.deb");
        blocking.save(
            key, new ByteArray(content.getBytes(StandardCharsets.UTF_8)).primitiveBytes()
        );
        final byte[] bytes = blocking.value(key);
        MatcherAssert.assertThat(
            new String(bytes, StandardCharsets.UTF_8),
            Matchers.equalTo(content)
        );
    }

    @Test
    void move() throws Exception {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key source = new Key.From("from");
        blocking.save(source, data);
        final Key destination = new Key.From("to");
        blocking.move(source, destination);
        MatcherAssert.assertThat(
            blocking.value(destination),
            Matchers.equalTo(data)
        );
    }

    @Test
    @EnabledIfSystemProperty(named = "test.storage.file.huge", matches = "true|on")
    @Timeout(1L)
    void saveAndLoadHugeFiles() throws Exception {
        final String name = "huge";
        new FileStorage(this.tmp).save(
            new Key.From(name),
            new Content.OneTime(
                new Content.From(
                    // @checkstyle MagicNumberCheck (1 line)
                    Flowable.generate(new WriteTestSource(1024 * 8, 1024 * 1024 / 8))
                )
            )
        ).get();
        MatcherAssert.assertThat(
            Files.size(this.tmp.resolve(name)),
            // @checkstyle MagicNumberCheck (1 line)
            Matchers.equalTo(1024L * 1024 * 1024)
        );
    }

    @Test
    void deletesFileDoesNotTouchEmptyStorageRoot() {
        final Key.From file = new Key.From("file.txt");
        this.storage.save(file, Content.EMPTY).join();
        this.storage.delete(file).join();
        MatcherAssert.assertThat(
            Files.exists(this.tmp),
            new IsEqual<>(true)
        );
    }

    @Test
    void deletesFileAndEmptyDirs() throws IOException {
        final Key.From file = new Key.From("one/two/file.txt");
        this.storage.save(file, Content.EMPTY).join();
        this.storage.delete(file).join();
        MatcherAssert.assertThat(
            "Storage root dir exists",
            Files.exists(this.tmp),
            new IsEqual<>(true)
        );
        try (Stream<Path> files = Files.list(this.tmp)) {
            MatcherAssert.assertThat(
                "All empty dirs removed",
                files.findFirst().isPresent(),
                new IsEqual<>(false)
            );
        }
    }

    @Test
    void deletesFileAndDoesNotTouchNotEmptyDirs() throws IOException {
        final Key.From file = new Key.From("one/two/file.txt");
        this.storage.save(file, Content.EMPTY).join();
        this.storage.save(new Key.From("one/file.txt"), Content.EMPTY).join();
        this.storage.delete(file).join();
        MatcherAssert.assertThat(
            "Storage root dir exists",
            Files.exists(this.tmp),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Another item exists",
            Files.exists(this.tmp.resolve("one/file.txt")),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsIdentifier() {
        MatcherAssert.assertThat(
            this.storage.identifier(),
            Matchers.stringContainsInOrder("FS", this.tmp.toString())
        );
    }

    /**
     * Create a directory.
     * @param parent Directory parent path
     * @param dirname Directory name
     * @return Path of directory
     */
    private static Path createNewDirectory(final Path parent, final String dirname) {
        final Path dir = parent.resolve(dirname);
        dir.toFile().mkdirs();
        return dir;
    }

    /**
     * Create a file.
     * @param parent Parent file path
     * @param filename File name
     * @return File created
     * @throws IOException If fails to create
     */
    private static File createNewFile(
        final Path parent,
        final String filename
    ) throws IOException {
        final File file = parent.resolve(filename).toFile();
        file.createNewFile();
        return file;
    }

    /**
     * Provider of byte buffers for write test.
     * @since 0.2
     */
    private static final class WriteTestSource implements Consumer<Emitter<ByteBuffer>> {

        /**
         * Counter.
         */
        private final AtomicInteger cnt;

        /**
         * Amount of buffers.
         */
        private final int length;

        /**
         * Buffer size.
         */
        private final int size;

        /**
         * New test source.
         * @param size Buffer size
         * @param length Amount of buffers
         */
        WriteTestSource(final int size, final int length) {
            this.cnt = new AtomicInteger();
            this.size = size;
            this.length = length;
        }

        @Override
        public void accept(final Emitter<ByteBuffer> src) {
            final int val = this.cnt.getAndIncrement();
            if (val < this.length) {
                final byte[] data = new byte[this.size];
                Arrays.fill(data, (byte) val);
                src.onNext(ByteBuffer.wrap(data));
            } else {
                src.onComplete();
            }
        }
    }
}
