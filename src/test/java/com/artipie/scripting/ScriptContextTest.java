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

/**
 * Tests for {@link ScriptContext}.
 * @since 0.30
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
class ScriptContextTest {

    @Test
    void testCache() {
        final LoadingCache<Key, Script.PrecompiledScript> scripts;
        final BlockingStorage storage = new BlockingStorage(new InMemoryStorage());
        scripts = ScriptContext.createCache(storage);
        final Key.From keya = new Key.From("usr/bin/testA.groovy");
        final Key.From keyb = new Key.From("usr/bin/testB.groovy");
        final String srccode = "a = a * 3;\nb = a + 1;";
        storage.save(keya, srccode.getBytes());
        storage.save(keyb, srccode.getBytes());
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(0L));
        final Script.PrecompiledScript saa = scripts.getUnchecked(keya);
        MatcherAssert.assertThat(saa, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(1L));
        final Script.PrecompiledScript sab = scripts.getUnchecked(keya);
        MatcherAssert.assertThat(sab, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(1L));
        MatcherAssert.assertThat(saa, new IsEqual<>(sab));
        final Script.PrecompiledScript sba = scripts.getUnchecked(keyb);
        MatcherAssert.assertThat(sba, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(2L));
        final Script.PrecompiledScript sbb = scripts.getUnchecked(keyb);
        MatcherAssert.assertThat(sbb, new IsNot<>(new IsEqual<>(null)));
        MatcherAssert.assertThat(scripts.size(), new IsEqual<>(2L));
        MatcherAssert.assertThat(sba, new IsEqual<>(sbb));
        MatcherAssert.assertThat(sba, new IsNot<>(new IsEqual<>(saa)));
        MatcherAssert.assertThat(sbb, new IsNot<>(new IsEqual<>(sab)));
    }
}
