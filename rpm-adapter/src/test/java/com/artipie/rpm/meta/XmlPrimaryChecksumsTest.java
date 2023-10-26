/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link XmlPrimaryChecksums}.
 * @since 0.8
 */
public class XmlPrimaryChecksumsTest {

    @Test
    void readsChecksums() {
        MatcherAssert.assertThat(
            new XmlPrimaryChecksums(new TestResource("repodata/primary.xml.example").asPath())
                .read().entrySet(),
            Matchers.hasItems(
                new MapEntry<>(
                    "aom-1.0.0-8.20190810git9666276.el8.aarch64.rpm",
                    "7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44"
                ),
                new MapEntry<>(
                    "nginx-1.16.1-1.el8.ngx.x86_64.rpm",
                    "54f1d9a1114fa85cd748174c57986004857b800fe9545fbf23af53f4791b31e2"
                )
            )
        );
    }

}
