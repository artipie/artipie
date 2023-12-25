/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoggingStorage}.
 *
 * @since 0.20.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class LoggingStorageTest {

    /**
     * Memory storage used in tests.
     */
    private InMemoryStorage memsto;

    /**
     * Logging storage to test.
     */
    private LoggingStorage logsto;

    /**
     * Log writer.
     */
    private StringWriter writer;

    @BeforeEach
    void setUp() {
        this.memsto = new InMemoryStorage();
        final Level level = Level.FINE;
        this.logsto = new LoggingStorage(level, this.memsto);
        this.writer = new StringWriter();
        final Logger logger = LogManager.getLogManager()
            .getLogger(this.memsto.getClass().getCanonicalName());
        logger.addHandler(new StringWriterHandler(this.writer));
        logger.setLevel(level);
    }

    @Test
    void retrievesKeyExistingInOriginalStorage() {
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        this.memsto.save(key, content).join();
        MatcherAssert.assertThat(
            "Should retrieve key existing in original storage",
            this.logsto.exists(key).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Should log after checking existence",
            this.writer.toString(),
            new IsEqual<>(String.format("Exists '%s': true", key))
        );
    }

    // @checkstyle MissingDeprecatedCheck (5 lines)
    @Test
    @Deprecated
    void readsSize() {
        final Key key = new Key.From("withSize");
        final byte[] data = new byte[]{0x00, 0x00, 0x00};
        final long dlg = data.length;
        this.memsto.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should read the size",
            this.logsto.size(key).join(),
            new IsEqual<>(dlg)
        );
        MatcherAssert.assertThat(
            "Should log after reading size",
            this.writer.toString(),
            new IsEqual<>(String.format("Size '%s': %s", key, dlg))
        );
    }

    @Test
    void savesContent() {
        final byte[] data = "01201".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("binary-key");
        this.logsto.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should save content",
            new BlockingStorage(this.memsto).value(key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after saving content",
            this.writer.toString(),
            new IsEqual<>(String.format("Save '%s': %s", key, Optional.of(data.length)))
        );
    }

    @Test
    void loadsContent() {
        final Key key = new Key.From("url");
        final byte[] data = "https://www.artipie.com"
            .getBytes(StandardCharsets.UTF_8);
        this.memsto.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should load content",
            new BlockingStorage(this.logsto).value(key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after loading content",
            this.writer.toString(),
            new IsEqual<>(
                String.format("Value '%s': %s", key, Optional.of(data.length))
            )
        );
    }

    @Test
    void listsItems() {
        final Key prefix = new Key.From("pref");
        final Key one = new Key.From(prefix, "one");
        final Key two = new Key.From(prefix, "two");
        this.memsto.save(one, Content.EMPTY).join();
        this.memsto.save(two, Content.EMPTY).join();
        final Collection<Key> keys =
            this.logsto.list(prefix).join();
        MatcherAssert.assertThat(
            "Should list items",
            keys,
            Matchers.hasItems(one, two)
        );
        MatcherAssert.assertThat(
            "Should log after listing items",
            this.writer.toString(),
            new IsEqual<>(String.format("List '%s': %s", prefix, keys.size()))
        );
    }

    @Test
    void movesContent() {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final Key source = new Key.From("from");
        this.memsto.save(source, new Content.From(data)).join();
        final Key destination = new Key.From("to");
        this.logsto.move(source, destination).join();
        MatcherAssert.assertThat(
            "Should move content",
            new BlockingStorage(this.memsto).value(destination),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after moving content",
            this.writer.toString(),
            new IsEqual<>(String.format("Move '%s' '%s'", source, destination))
        );
    }

    @Test
    void deletesContent() {
        final byte[] data = "my file content".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("filename");
        this.memsto.save(key, new Content.From(data)).join();
        this.logsto.delete(key).join();
        MatcherAssert.assertThat(
            "Should delete content",
            new BlockingStorage(this.memsto).exists(key),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Should log after deleting content",
            this.writer.toString(),
            new IsEqual<>(String.format("Delete '%s'", key))
        );
    }

    @Test
    void retrievesMetadata() {
        final Key key = new Key.From("page");
        final byte[] data = "Wiki content".getBytes(StandardCharsets.UTF_8);
        final long dlg = data.length;
        this.memsto.save(key, new Content.From(data)).join();
        final Meta metadata = this.logsto.metadata(key).join();
        MatcherAssert.assertThat(
            "Should retrieve metadata size",
            metadata.read(Meta.OP_SIZE).get(),
            new IsEqual<>(dlg)
        );
        MatcherAssert.assertThat(
            "Should log after retrieving metadata",
            this.writer.toString(),
            new IsEqual<>(String.format("Metadata '%s': %s", key, metadata))
        );
    }

    @Test
    void shouldRunExclusively() {
        final Key key = new Key.From("key-exc");
        final Function<Storage, CompletionStage<Boolean>> operation =
            sto -> CompletableFuture.completedFuture(true);
        this.memsto.save(key, Content.EMPTY).join();
        final Boolean finished = this.logsto.exclusively(key, operation)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Should run exclusively",
            finished,
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Should log after running exclusively",
            this.writer.toString(),
            new IsEqual<>(String.format("Exclusively for '%s': %s", key, operation))
        );
    }

    /**
     * String writer handler.
     *
     * @since 1.11
     */
    private final class StringWriterHandler extends Handler {

        /**
         * String writer.
         */
        private final StringWriter writer;

        /**
         * Ctor.
         * @param writer String writer
         */
        StringWriterHandler(final StringWriter writer) {
            this.writer = writer;
        }

        @Override
        public void publish(final LogRecord record) {
            this.writer.append(record.getMessage());
        }

        @Override
        public void flush() {
            this.writer.flush();
        }

        @Override
        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        public void close() throws SecurityException {
            try {
                this.writer.close();
            } catch (final IOException exe) {
                throw new RuntimeException(exe);
            }
        }
    }
}
