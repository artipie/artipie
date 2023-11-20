/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.asto.test.TestResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Debian.Asto}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @todo #51:30min Let's create a class in test scope to held/obtain information about test .deb
 *  packages, the class should provide package name, bytes, be able to put the package into provided
 *  storage and return meta info (like methods in this class do). We something similar in
 *  rpm-adapter, check https://github.com/artipie/artipie/blob/master/src/test/java/com/artipie/rpm/TestRpm.java
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AssignmentInOperand"})
class DebianTest {

    /**
     * Debian repository name.
     */
    private static final String NAME = "my_deb_repo";

    /**
     * Packages index key.
     */
    private static final Key PACKAGES =
        new Key.From(DebianTest.NAME, "binary", "amd64", "Packages.gz");

    /**
     * Release file lines.
     */
    private static final ListOf<String> RELEASE_LINES = new ListOf<>(
        String.format("Codename: %s", DebianTest.NAME),
        "Architectures: amd64",
        "Components: main",
        "Date:",
        "SHA256:",
        "my_deb_repo/binary/amd64/Packages.gz",
        "my_deb_repo/binary/amd64/Packages"
    );

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Debian test instance.
     */
    private Debian debian;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        final String key = "secret-keys.gpg";
        final Storage settings = new InMemoryStorage();
        new TestResource(key).saveTo(settings);
        this.debian = new Debian.Asto(
            this.storage,
            new Config.FromYaml(
                DebianTest.NAME,
                Yaml.createYamlMappingBuilder()
                    .add("Components", "main")
                    .add("Architectures", "amd64")
                    .add("gpg_password", "1q2w3e4r5t6y7u")
                    .add("gpg_secret_key", key).build(),
                settings
            )
        );
    }

    @Test
    void addsPackagesAndReleaseIndexes() throws IOException {
        final List<String> debs = new ListOf<>(
            "libobus-ocaml_1.2.3-1+b3_amd64.deb",
            "aglfn_1.7-3_amd64.deb"
        );
        final String prefix = "my_deb";
        debs.forEach(
            item -> new TestResource(item).saveTo(this.storage, new Key.From(prefix, item))
        );
        this.debian.updatePackages(
            debs.stream().map(item -> new Key.From(prefix, item)).collect(Collectors.toList()),
            DebianTest.PACKAGES
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Generates Packages index",
            new AstoGzArchive(this.storage).unpack(DebianTest.PACKAGES),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains("\n\n"),
                    new StringContains(this.libobusOcaml()),
                    new StringContains(this.aglfn())
                )
            )
        );
        final Key release = this.debian.generateRelease().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Generates Release index",
            new PublisherAs(this.storage.value(release).join()).asciiString()
                .toCompletableFuture().join(),
            new StringContainsInOrder(DebianTest.RELEASE_LINES)
        );
        MatcherAssert.assertThat(
            "Generates Release.gpg file",
            this.storage.exists(new Key.From(String.format("%s.gpg", release.string()))).join(),
            new IsEqual<>(true)
        );
        this.debian.generateInRelease(release).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Generates InRelease index",
            new PublisherAs(
                this.storage.value(new Key.From("dists", DebianTest.NAME, "InRelease")).join()
            ).asciiString().toCompletableFuture().join(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContainsInOrder(DebianTest.RELEASE_LINES),
                    new StringContains("-----BEGIN PGP SIGNED MESSAGE-----"),
                    new StringContains("Hash: SHA256"),
                    new StringContains("-----BEGIN PGP SIGNATURE-----"),
                    new StringContains("-----END PGP SIGNATURE-----")
                )
            )
        );
    }

    @Test
    void updatesPackagesIndexAndReleaseFile() throws IOException {
        final String pckg = "pspp_1.2.0-3_amd64.deb";
        final Key.From key = new Key.From("some_repo", pckg);
        new TestResource(pckg).saveTo(this.storage, key);
        new AstoGzArchive(this.storage).packAndSave(this.aglfn(), DebianTest.PACKAGES);
        this.debian.updatePackages(new ListOf<>(key), DebianTest.PACKAGES)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Packages index was updated",
            new AstoGzArchive(this.storage).unpack(DebianTest.PACKAGES),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains("\n\n"),
                    new StringContains(this.pspp()),
                    new StringContains(this.aglfn())
                )
            )
        );
        this.storage.save(
            new Key.From("dists", DebianTest.NAME, "Release"),
            new Content.From(this.release().getBytes(StandardCharsets.UTF_8))
        ).join();
        final byte[] bytes = "any".getBytes();
        this.storage.save(
            new Key.From("dists", DebianTest.NAME, "Release.gpg"),
            new Content.From(bytes)
        ).join();
        final Key release = this.debian.updateRelease(DebianTest.PACKAGES)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Updates Release index",
            new PublisherAs(this.storage.value(release).join()).asciiString()
                .toCompletableFuture().join(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContainsInOrder(DebianTest.RELEASE_LINES),
                    new IsNot<>(
                        new StringContains("abc123 123 my_deb_repo/binary/amd64/Packages.gz")
                    ),
                    new IsNot<>(
                        new StringContains("098xyz 234 my_deb_repo/binary/amd64/Packages")
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Generates new gpg signature",
            this.storage.value(new Key.From(String.format("%s.gpg", release.string()))).join(),
            new IsNot<>(new ContentIs(bytes))
        );
    }

    private String release() {
        return String.join(
            "\n",
            String.format("Codename: %s", DebianTest.NAME),
            "Architectures: amd64",
            "Components: main",
            "Date:",
            "SHA256:",
            " abc123 123 my_deb_repo/binary/amd64/Packages.gz",
            " 098xyz 234 my_deb_repo/binary/amd64/Packages"
        );
    }

    private String pspp() {
        return String.join(
            "\n",
            "Package: pspp",
            "Version: 1.2.0-3",
            "Architecture: amd64",
            "Maintainer: Debian Science Team <debian-science-maintainers@lists.alioth.debian.org>",
            "Installed-Size: 15735",
            // @checkstyle LineLengthCheck (1 line)
            "Depends: libatk1.0-0 (>= 1.12.4), libc6 (>= 2.17), libcairo-gobject2 (>= 1.10.0), libcairo2 (>= 1.12), libgdk-pixbuf2.0-0 (>= 2.22.0), libglib2.0-0 (>= 2.43.4), libgsl23 (>= 2.5), libgslcblas0 (>= 2.4), libgtk-3-0 (>= 3.21.5), libgtksourceview-3.0-1 (>= 3.18), libpango-1.0-0 (>= 1.22), libpangocairo-1.0-0 (>= 1.22), libpq5, libreadline7 (>= 6.0), libspread-sheet-widget, libxml2 (>= 2.7.4), zlib1g (>= 1:1.1.4), emacsen-common",
            "Section: math",
            "Priority: optional",
            "Homepage: http://savannah.gnu.org/projects/pspp",
            "Description: Statistical analysis tool",
            " PSPP is a program for statistical analysis of sampled data. It is a free",
            " replacement for the proprietary program SPSS.",
            " .",
            " PSPP supports T-tests, ANOVA, GLM, factor analysis, non-parametric tests, and",
            " other statistical features. PSPP produces statistical reports in plain text,",
            " PDF, PostScript, CSV, HTML, SVG, and OpenDocument formats.",
            " .",
            " PSPP has both text-based and graphical user interfaces. The PSPP user interface",
            " has been translated into a number of languages.",
            "Filename: some_repo/pspp_1.2.0-3_amd64.deb",
            "Size: 3809960",
            "MD5sum: 42f4ff59934206b37574fc317b94a854",
            "SHA1: ec07cc41c41f0db4c287811d05564ad8c6ca1845",
            "SHA256: 02b15744576cefe92a1f874d8663575caaa71c0e6c60795e8617c23338fc5fc3"
        );
    }

    private String aglfn() {
        return String.join(
            "\n",
            "Package: aglfn",
            "Version: 1.7-3",
            "Architecture: amd64",
            "Maintainer: Debian Fonts Task Force <pkg-fonts-devel@lists.alioth.debian.org>",
            "Installed-Size: 138",
            "Section: fonts",
            "Priority: extra",
            "Homepage: http://sourceforge.net/adobe/aglfn/",
            "Description: Adobe Glyph List For New Fonts",
            " AGL (Adobe Glyph List) maps glyph names to Unicode values for the",
            " purpose of deriving content. AGLFN (Adobe Glyph List For New Fonts) is a",
            " subset of AGL that excludes the glyph names associated with the PUA",
            " (Private Use Area), and is meant to specify preferred glyph names for",
            " new fonts. Also included is the ITC Zapf Dingbats Glyph List, which is",
            " similar to AGL in that it maps glyph names to Unicode values for the",
            " purpose of deriving content, but only for the glyphs in the ITC Zapf",
            " Dingbats font.",
            " .",
            " Be sure to visit the AGL Specification and Developer Documentation pages",
            " for detailed information about naming glyphs, interpreting glyph names,",
            " and developing OpenType fonts.",
            "Filename: my_deb/aglfn_1.7-3_amd64.deb",
            "Size: 29936",
            "MD5sum: eb647d864e8283cbf5b17e44a2a00b9c",
            "SHA1: 246ffaf3e5e06259e663d404f16764171216c538",
            "SHA256: 66f92b0628fb5fcbc76b9e1388f4f4d1ebf5a68835f05a03a876e08c56f46ab3"
        );
    }

    private String libobusOcaml() {
        return String.join(
            "\n",
            "Package: libobus-ocaml",
            "Source: obus (1.2.3-1)",
            "Version: 1.2.3-1+b3",
            "Architecture: amd64",
            "Maintainer: Debian OCaml Maintainers <debian-ocaml-maint@lists.debian.org>",
            "Installed-Size: 5870",
            // @checkstyle LineLengthCheck (1 line)
            "Depends: liblwt-log-ocaml-1f1y2, liblwt-ocaml-dt6l9, libmigrate-parsetree-ocaml-n2039, libreact-ocaml-pdm50, libresult-ocaml-ki2r2, libsexplib0-ocaml-drlz0, ocaml-base-nox-4.11.1",
            "Provides: libobus-ocaml-d0567",
            "Section: ocaml",
            "Priority: optional",
            "Homepage: https://github.com/ocaml-community/obus",
            "Description: pure OCaml implementation of D-Bus (runtime)",
            " OBus is a pure OCaml implementation of D-Bus. It aims to provide a",
            " clean and easy way for OCaml programmers to access and provide D-Bus",
            " services.",
            " .",
            " This package contains dynamically loadable plugins of OBus.",
            "Filename: my_deb/libobus-ocaml_1.2.3-1+b3_amd64.deb",
            "Size: 1338616",
            "MD5sum: 2121df46da5e94bb68603bb2f573d80b",
            "SHA1: b61297f47c6d8c8bb530301cd915e05e2bd23365",
            "SHA256: 90dce70b7604a4e3a35faa35830039af203c7b8df5399ef0eab818157f5c4ce6"
        );
    }

}
