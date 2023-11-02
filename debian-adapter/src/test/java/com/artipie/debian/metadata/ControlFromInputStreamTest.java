/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.test.TestResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Control.FromInputStream}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ControlFromInputStreamTest {

    @Test
    void readsDataFromTarGz() {
        MatcherAssert.assertThat(
            new Control.FromInputStream(new TestResource("aglfn_1.7-3_all.deb").asInputStream())
                .asString(),
            new IsEqual<>(
                String.join(
                    "\n",
                    "Package: aglfn",
                    "Version: 1.7-3",
                    "Architecture: all",
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
                    " and developing OpenType fonts.\n"
                )
            )
        );
    }

    @Test
    void readsDataFromTarXz() {
        MatcherAssert.assertThat(
            new Control.FromInputStream(new TestResource("pspp_1.2.0-3_amd64.deb").asInputStream())
                .asString(),
            new IsEqual<>(
                String.join(
                    "\n",
                    // @checkstyle LineLengthCheck (50 lines)
                    "Package: pspp",
                    "Version: 1.2.0-3",
                    "Architecture: amd64",
                    "Maintainer: Debian Science Team <debian-science-maintainers@lists.alioth.debian.org>",
                    "Installed-Size: 15735",
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
                    " has been translated into a number of languages.\n"
                )
            )
        );
    }

    @Test
    void readsDataFromZst() {
        MatcherAssert.assertThat(
            new Control.FromInputStream(new TestResource("test_3.0_1_amd64.deb").asInputStream())
                .asString(),
            new IsEqual<>(
                String.join(
                    "\n",
                    "Package: consulo",
                    "Version: 3.0",
                    "Architecture: amd64",
                    "Maintainer: consulo.io <admin@consulo.io>",
                    "Description: Multilanguage IDE\n"
                )
            )
        );
    }

}
