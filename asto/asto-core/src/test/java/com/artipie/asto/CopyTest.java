/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link Copy}.
 * @since 0.19
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
 */
public class CopyTest {

    @Test
    public void copyTwoFilesFromOneStorageToAnotherWorksFine()
        throws ExecutionException, InterruptedException {
        final Storage from = new InMemoryStorage();
        final Storage to = new InMemoryStorage();
        final Key akey = new Key.From("a.txt");
        final Key bkey = new Key.From("b.txt");
        final BlockingStorage bfrom = new BlockingStorage(from);
        bfrom.save(akey, "Hello world A".getBytes());
        bfrom.save(bkey, "Hello world B".getBytes());
        new Copy(from, Arrays.asList(akey, bkey)).copy(to).get();
        for (final Key key : new BlockingStorage(from).list(Key.ROOT)) {
            MatcherAssert.assertThat(
                Arrays.equals(
                    bfrom.value(key),
                    new BlockingStorage(to).value(key)
                ),
                Matchers.is(true)
            );
        }
    }

    @Test
    public void copyEverythingFromOneStorageToAnotherWorksFine() {
        final Storage from = new InMemoryStorage();
        final Storage to = new InMemoryStorage();
        final Key akey = new Key.From("a/b/c");
        final Key bkey = new Key.From("foo.bar");
        final BlockingStorage bfrom = new BlockingStorage(from);
        bfrom.save(akey, "one".getBytes());
        bfrom.save(bkey, "two".getBytes());
        new Copy(from).copy(to).join();
        for (final Key key : bfrom.list(Key.ROOT)) {
            MatcherAssert.assertThat(
                new BlockingStorage(to).value(key),
                new IsEqual<>(bfrom.value(key))
            );
        }
    }

    @Test
    public void copyPredicate() {
        final Storage src = new InMemoryStorage();
        final Storage dst = new InMemoryStorage();
        final Key foo = new Key.From("foo");
        new BlockingStorage(src).save(foo, new byte[]{0x00});
        new BlockingStorage(src).save(new Key.From("bar/baz"), new byte[]{0x00});
        new Copy(src, key -> key.string().contains("oo")).copy(dst).join();
        MatcherAssert.assertThat(
            new BlockingStorage(dst).list(Key.ROOT),
            Matchers.contains(foo)
        );
    }
}
