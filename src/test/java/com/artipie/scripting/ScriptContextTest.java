/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.google.common.cache.LoadingCache;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Tests for {@link ScriptContext}.
 * @since 0.30
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
class ScriptContextTest {

    @Test
    void testFcEqual() {
        final String path = "/usr/bin/test1";
        final Key.From keya = new Key.From(path);
        final Key.From keyb = new Key.From(path);
        final BlockingStorage storage = new BlockingStorage(new InMemoryStorage());
        final ScriptContext.FilesContent fca = new ScriptContext.FilesContent(keya, storage);
        final ScriptContext.FilesContent fcb = new ScriptContext.FilesContent(keyb, storage);
        MatcherAssert.assertThat(fca.equals(fcb), new IsTrue());
        MatcherAssert.assertThat(fca.hashCode(), new IsEqual<>(fcb.hashCode()));
    }

    @Test
    void testFcNotEqual() {
        final Key.From keya = new Key.From("/usr/bin/testA");
        final Key.From keyb = new Key.From("/usr/bin/testB");
        final BlockingStorage storage = new BlockingStorage(new InMemoryStorage());
        final ScriptContext.FilesContent fca = new ScriptContext.FilesContent(keya, storage);
        final ScriptContext.FilesContent fcb = new ScriptContext.FilesContent(keyb, storage);
        MatcherAssert.assertThat(fca.equals(fcb), new IsEqual<>(false));
        MatcherAssert.assertThat(fca.hashCode(), new IsNot<>(new IsEqual<>(fcb.hashCode())));
    }

    @Test
    void testDifferentStoragesDontCompareInFC() {
        final String path = "/usr/bin/test2";
        final Key.From keya = new Key.From(path);
        final Key.From keyb = new Key.From(path);
        final BlockingStorage storagea = new BlockingStorage(new InMemoryStorage());
        final BlockingStorage storageb = new BlockingStorage(new InMemoryStorage());
        final ScriptContext.FilesContent fca = new ScriptContext.FilesContent(keya, storagea);
        final ScriptContext.FilesContent fcb = new ScriptContext.FilesContent(keyb, storageb);
        MatcherAssert.assertThat(storagea.equals(storageb), new IsEqual<>(false));
        MatcherAssert.assertThat(
            storagea.hashCode(), new IsNot<>(new IsEqual<>(storageb.hashCode()))
        );
        MatcherAssert.assertThat(fca.equals(fcb), new IsTrue());
        MatcherAssert.assertThat(fca.hashCode(), new IsEqual<>(fcb.hashCode()));
    }

    @Test
    void testCache() {
        final LoadingCache<ScriptContext.FilesContent, Script.PrecompiledScript> scripts;
        scripts = ScriptContext.createCache();
        final Key.From keya = new Key.From("usr/bin/testA.groovy");
        final Key.From keyb = new Key.From("usr/bin/testB.groovy");
        final String srccode = "a = a * 3;\nb = a + 1;";
        final BlockingStorage storage = new BlockingStorage(new InMemoryStorage());
        storage.save(keya, srccode.getBytes());
        storage.save(keyb, srccode.getBytes());
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(0L));
        final Script.PrecompiledScript saa = scripts.getUnchecked(
            new ScriptContext.FilesContent(keya, storage)
        );
        MatcherAssert.assertThat(saa, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(1L));
        final Script.PrecompiledScript sab = scripts.getUnchecked(
            new ScriptContext.FilesContent(keya, storage)
        );
        MatcherAssert.assertThat(sab, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(1L));
        MatcherAssert.assertThat(saa, new IsEqual<>(sab));
        final Script.PrecompiledScript sba = scripts.getUnchecked(
            new ScriptContext.FilesContent(keyb, storage)
        );
        MatcherAssert.assertThat(sba, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(2L));
        final Script.PrecompiledScript sbb = scripts.getUnchecked(
            new ScriptContext.FilesContent(keyb, storage)
        );
        MatcherAssert.assertThat(sbb, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(2L));
        MatcherAssert.assertThat(sba, new IsEqual<>(sbb));
        MatcherAssert.assertThat(sba, new IsNot<>(new IsEqual<>(saa)));
        MatcherAssert.assertThat(sbb, new IsNot<>(new IsEqual<>(sab)));
    }
}
