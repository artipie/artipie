/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.fs.RxFile;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link RxFile}.
 * @since 0.11.1
 */
final class RxFileTest {

    @Test
    @Timeout(1)
    public void rxFileFlowWorks(@TempDir final Path tmp) throws IOException {
        final String hello = "hello-world";
        final Path temp = tmp.resolve("txt-file");
        Files.write(temp, hello.getBytes());
        final String content = new RxFile(temp)
            .flow()
            .rebatchRequests(1)
            .toList()
            .map(
                list -> list.stream().map(buf -> new Remaining(buf).bytes())
                    .flatMap(byteArr -> Arrays.stream(new ByteArray(byteArr).boxedBytes()))
                    .toArray(Byte[]::new)
            )
            .map(bytes -> new String(new ByteArray(bytes).primitiveBytes()))
            .blockingGet();
        MatcherAssert.assertThat(hello, Matchers.equalTo(content));
    }

    @Test
    @Timeout(1)
    public void rxFileTruncatesExistingFile(@TempDir final Path tmp) throws Exception {
        final String one = "one";
        final String two = "two111";
        final Path target = tmp.resolve("target.txt");
        new RxFile(target).save(pubFromString(two)).blockingAwait();
        new RxFile(target).save(pubFromString(one)).blockingAwait();
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(target), StandardCharsets.UTF_8),
            Matchers.equalTo(one)
        );
    }

    @Test
    @Timeout(1)
    public void rxFileSaveWorks(@TempDir final Path tmp) throws IOException {
        final String hello = "hello-world!!!";
        final Path temp = tmp.resolve("saved.txt");
        new RxFile(temp).save(
            Flowable.fromArray(new ByteArray(hello.getBytes()).boxedBytes()).map(
                aByte -> {
                    final byte[] bytes = new byte[1];
                    bytes[0] = aByte;
                    return ByteBuffer.wrap(bytes);
                }
            )
        ).blockingAwait();
        MatcherAssert.assertThat(new String(Files.readAllBytes(temp)), Matchers.equalTo(hello));
    }

    @Test
    @Timeout(1)
    public void rxFileSizeWorks(@TempDir final Path tmp) throws IOException {
        final byte[] data = "012345".getBytes();
        final Path temp = tmp.resolve("size-test.txt");
        Files.write(temp, data);
        final Long size = new RxFile(temp).size().blockingGet();
        MatcherAssert.assertThat(
            size,
            Matchers.equalTo((long) data.length)
        );
    }

    /**
     * Creates publisher of byte buffers from string using UTF8 encoding.
     * @param str Source string
     * @return Publisher
     */
    private static Flowable<ByteBuffer> pubFromString(final String str) {
        return Flowable.fromArray(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }
}
