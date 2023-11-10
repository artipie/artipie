/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.Checksum;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlRepomd}.
 * @since 0.11
 */
class XmlRepomdTest {

    @Test
    void writesRepomd(@TempDir final Path temp) throws Exception {
        final Path res = temp.resolve("repomd.xml");
        final long time = System.currentTimeMillis();
        final String type = "some_data";
        final long gzsize = 10;
        final long size = 20;
        final String gzhex = "abc";
        final String hex = "123";
        final String loc = "temp/some_data";
        try (XmlRepomd repomd = new XmlRepomd(res)) {
            repomd.begin(time);
            try (XmlRepomd.Data data = repomd.beginData(type)) {
                data.gzipSize(gzsize);
                data.openSize(size);
                data.gzipChecksum(new Checksum.Simple(Digest.SHA256, gzhex));
                data.openChecksum(new Checksum.Simple(Digest.SHA1, hex));
                data.location(loc);
            }
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(res), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (15 lines)
            XhtmlMatchers.hasXPaths(
                String.format("/*[local-name()='repomd']/*[local-name()='revision' and text()='%s']", time),
                String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='%s']", type),
                String.format("/*[local-name()='repomd']/*[local-name()='data']/*[local-name()='checksum' and @type='%s' and text()='%s']", Digest.SHA256.type(), gzhex),
                String.format("/*[local-name()='repomd']/*[local-name()='data']/*[local-name()='open-checksum' and @type='%s' and text()='%s']", Digest.SHA1.type(), hex),
                String.format("/*[local-name()='repomd']/*[local-name()='data']/*[local-name()='location' and @href='%s']", loc),
                String.format("/*[local-name()='repomd']/*[local-name()='data']/*[local-name()='open-size' and text()='%s']", size),
                String.format("/*[local-name()='repomd']/*[local-name()='data']/*[local-name()='size' and text()='%s']", gzsize)
            )
        );
    }
}
