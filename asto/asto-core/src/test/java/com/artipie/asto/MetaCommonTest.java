/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link MetaCommon}.
 * @since 1.11
 */
final class MetaCommonTest {

    @Test
    void readsSize() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("key");
        final String data = "012004407";
        storage.save(
            key,
            new Content.From(data.getBytes(StandardCharsets.UTF_8))
        );
        MatcherAssert.assertThat(
            "Gets value size from metadata",
            new MetaCommon(storage.metadata(key).join()).size(),
            new IsEqual<>(Long.valueOf(data.length()))
        );
    }

}
