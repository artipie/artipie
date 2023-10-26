/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.hm.IsXmlEqual;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlPrimaryMaid}.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlPrimaryMaidTest {

    @Test
    void clearsFirstItem(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestResource("repodata/primary.xml.example").asPath(),
            temp.resolve("primary1.xml")
        );
        new XmlPrimaryMaid(file).clean(
            new ListOf<>("7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44")
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestResource("repodata/primary.xml.example_maid.second").asPath())
        );
    }

    @Test
    void clearsLastItem(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestResource("repodata/primary.xml.example").asPath(),
            temp.resolve("primary2.xml")
        );
        new XmlPrimaryMaid(file).clean(
            new ListOf<>("54f1d9a1114fa85cd748174c57986004857b800fe9545fbf23af53f4791b31e2")
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestResource("repodata/primary.xml.example_maid.first").asPath())
        );
    }

}
