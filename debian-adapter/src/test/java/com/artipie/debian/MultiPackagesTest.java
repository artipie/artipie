/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MultiPackages.Unique}.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MultiPackagesTest {

    @Test
    void mergesPackages() throws IOException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        new MultiPackages.Unique().merge(
            new ListOf<InputStream>(
                this.stream(this.abcPackageInfo()),
                this.stream(this.xyzPackageInfo())
            ),
            res
        );
        MatcherAssert.assertThat(
            new GzArchive().decompress(res.toByteArray()),
            new IsEqual<>(
                String.join("\n\n", this.abcPackageInfo(), this.xyzPackageInfo(), "")
            )
        );
    }

    @Test
    void addsOnlyUniquePackages() throws IOException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        new MultiPackages.Unique().merge(
            new ListOf<InputStream>(
                this.stream(this.abcPackageInfo(), this.zeroPackageInfo()),
                this.stream(this.zeroPackageInfo()),
                this.stream(this.xyzPackageInfo(), this.zeroPackageInfo())
            ),
            res
        );
        MatcherAssert.assertThat(
            new GzArchive().decompress(res.toByteArray()),
            new IsEqual<>(
                String.join(
                    "\n\n", this.abcPackageInfo(), this.zeroPackageInfo(), this.xyzPackageInfo(), ""
                )
            )
        );
    }

    @Test
    void addsSamePackagesWithDiffVersions() throws IOException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final String two = this.abcPackageInfo().replace("0.1", "0.2");
        final String three = this.abcPackageInfo().replace("0.1", "0.3");
        new MultiPackages.Unique().merge(
            new ListOf<InputStream>(
                this.stream(two, this.abcPackageInfo()),
                this.stream(this.abcPackageInfo()),
                this.stream(two, three)
            ),
            res
        );
        MatcherAssert.assertThat(
            new GzArchive().decompress(res.toByteArray()),
            new IsEqual<>(String.join("\n\n", two, this.abcPackageInfo(), three, ""))
        );
    }

    @Test
    void handlesExtraLineBreaks() throws IOException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        new MultiPackages.Unique().merge(
            new ListOf<InputStream>(
                this.stream(String.join("\n\n\n", this.abcPackageInfo(), this.zeroPackageInfo())),
                this.stream(String.format("%s\n\n", this.xyzPackageInfo()))
            ),
            res
        );
        MatcherAssert.assertThat(
            new GzArchive().decompress(res.toByteArray()),
            new IsEqual<>(
                String.join(
                    "\n\n", this.abcPackageInfo(), this.zeroPackageInfo(), this.xyzPackageInfo(), ""
                )
            )
        );
    }

    private String xyzPackageInfo() {
        return String.join(
            "\n",
            "Package: xyz",
            "Version: 0.6",
            "Architecture: amd64",
            "Maintainer: Blue Sky",
            "Installed-Size: 131",
            "Section: Un-Existing",
            "Filename: some/not/existing/package.deb",
            "Size: 45",
            "MD5sum: e99a18c428cb38d5f260883678922e03"
        );
    }

    private String abcPackageInfo() {
        return String.join(
            "\n",
            "Package: abc",
            "Version: 0.1",
            "Architecture: all",
            "Maintainer: Task Force",
            "Installed-Size: 130",
            "Section: The Force",
            "Filename: my/repo/abc.deb",
            "Size: 23",
            "MD5sum: e99a18c428cb38d5f260853678922e03"
        );
    }

    private String zeroPackageInfo() {
        return String.join(
            "\n",
            "Package: zero",
            "Version: 0.0",
            "Architecture: all",
            "Maintainer: Zero division",
            "Installed-Size: 0",
            "Section: Zero",
            "Filename: zero/division/package.deb",
            "Size: 0",
            "MD5sum: 0000"
        );
    }

    private InputStream stream(final String... items) {
        return this.stream(StringUtils.join(items, "\n\n"));
    }

    private InputStream stream(final String item) {
        return new ByteArrayInputStream(
            new GzArchive().compress(item.getBytes(StandardCharsets.UTF_8))
        );
    }

}
