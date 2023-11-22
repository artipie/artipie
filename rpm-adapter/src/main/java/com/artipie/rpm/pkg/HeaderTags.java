/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.ArtipieException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.redline_rpm.header.Header;

/**
 * Helper object to read metadata header tags from RPM package.
 *
 * @since 0.6
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class HeaderTags {

    /**
     * Metadata.
     */
    private final Package.Meta meta;

    /**
     * Ctor.
     * @param meta Metadata
     */
    public HeaderTags(final Package.Meta meta) {
        this.meta = meta;
    }

    /**
     * Get the name header.
     * @return Value of header tag NAME.
     */
    public String name() {
        return this.meta.header(Header.HeaderTag.NAME).asString("");
    }

    /**
     * Get the arch header.
     * @return Value of header tag ARCH.
     */
    public String arch() {
        return this.meta.header(Header.HeaderTag.ARCH).asString("");
    }

    /**
     * Get the epoch header.
     * @return Value of header tag EPOCH.
     */
    public int epoch() {
        return this.meta.header(Header.HeaderTag.EPOCH).asInt(0);
    }

    /**
     * Get the version header.
     * @return Value of header tag VERSION.
     */
    public String version() {
        return this.meta.header(Header.HeaderTag.VERSION).asString("");
    }

    /**
     * Get the release header.
     * @return Value of header tag RELEASE.
     */
    public String release() {
        return this.meta.header(Header.HeaderTag.RELEASE).asString("");
    }

    /**
     * Get the summary header.
     * @return Value of header tag SUMMARY.
     */
    public String summary() {
        return this.meta.header(Header.HeaderTag.SUMMARY).asString("");
    }

    /**
     * Get the description header.
     * @return Value of header tag DESCRIPTION.
     */
    public String description() {
        return this.meta.header(Header.HeaderTag.DESCRIPTION).asString("");
    }

    /**
     * Get the package header.
     * @return Value of header tag PACKAGER.
     */
    public String packager() {
        return this.meta.header(Header.HeaderTag.PACKAGER).asString("");
    }

    /**
     * Get the url header.
     * @return Value of header tag URL.
     */
    public String url() {
        return this.meta.header(Header.HeaderTag.URL).asString("");
    }

    /**
     * Get the filemtimes header.
     * @return Value of header tag FILEMTIMES.
     */
    public int fileTimes() {
        return this.meta.header(Header.HeaderTag.FILEMTIMES).asInt(0);
    }

    /**
     * Get the build time header.
     * @return Value of header tag BUILDTIME.
     */
    public int buildTime() {
        return this.meta.header(Header.HeaderTag.BUILDTIME).asInt(0);
    }

    /**
     * Get the size header.
     * @return Value of header tag SIZE.
     */
    public int installedSize() {
        return this.meta.header(Header.HeaderTag.SIZE).asInt(0);
    }

    /**
     * Get the archive size header.
     * @return Value of header tag ARCHIVESIZE.
     */
    public int archiveSize() {
        return this.meta.header(Header.HeaderTag.ARCHIVESIZE).asInt(0);
    }

    /**
     * Get the license header.
     * @return Value of header tag LICENSE.
     */
    public String license() {
        return this.meta.header(Header.HeaderTag.LICENSE).asString("");
    }

    /**
     * Get the vendor header.
     * @return Value of header tag VENDOR.
     */
    public String vendor() {
        return this.meta.header(Header.HeaderTag.VENDOR).asString("");
    }

    /**
     * Get the group header.
     * @return Value of header tag GROUP.
     */
    public String group() {
        return this.meta.header(Header.HeaderTag.GROUP).asString("");
    }

    /**
     * Get the build host header.
     * @return Value of header tag BUILDHOST.
     */
    public String buildHost() {
        return this.meta.header(Header.HeaderTag.BUILDHOST).asString("");
    }

    /**
     * Get the source RPM header.
     * @return Value of header tag SOURCERPM.
     */
    public String sourceRmp() {
        return this.meta.header(Header.HeaderTag.SOURCERPM).asString("");
    }

    /**
     * Get the provides libraries names.
     * @return Value of header tag PROVIDENAME.
     */
    public List<String> providesNames() {
        return this.meta.header(Header.HeaderTag.PROVIDENAME).asStrings();
    }

    /**
     * Get the provides libraries versions.
     * @return Value of header tag PROVIDEVERSION.
     */
    public List<HeaderTags.Version> providesVer() {
        return this.meta.header(Header.HeaderTag.PROVIDEVERSION).asStrings().stream()
            .map(HeaderTags.Version::new).collect(Collectors.toList());
    }

    /**
     * Get the provides flags header.
     * @return Value of header tag PROVIDEFLAGS.
     */
    public List<Optional<String>> providesFlags() {
        final int[] array = this.meta.header(Header.HeaderTag.PROVIDEFLAGS).asInts();
        return Arrays.stream(array)
            .mapToObj(Flags::find).collect(Collectors.toList());
    }

    /**
     * Get the require name header.
     * @return Value of header tag REQUIRENAME.
     */
    public List<String> requires() {
        return this.meta.header(Header.HeaderTag.REQUIRENAME).asStrings();
    }

    /**
     * Get the require version header.
     * @return Value of header tag REQUIREVERSION.
     */
    public List<HeaderTags.Version> requiresVer() {
        return this.meta.header(Header.HeaderTag.REQUIREVERSION).asStrings().stream()
            .map(HeaderTags.Version::new).collect(Collectors.toList());
    }

    /**
     * Get the require flags header as strings.
     * @return Value of header tag REQUIREFLAGS.
     */
    public List<Optional<String>> requireFlags() {
        // @checkstyle MagicNumberCheck (1 line)
        return this.requireFlagsInts().stream().map(flag -> flag & 0xf)
            .map(Flags::find).collect(Collectors.toList());
    }

    /**
     * Get the obsolete name header.
     * @return Value of header tag OBSOLETENAME.
     */
    public List<String> obsoletes() {
        return this.meta.header(Header.HeaderTag.OBSOLETENAME).asStrings();
    }

    /**
     * Get the obsolete versions header.
     * @return Value of header tag OBSOLETEVERSION.
     */
    public List<HeaderTags.Version> obsoletesVer() {
        return this.meta.header(Header.HeaderTag.OBSOLETEVERSION).asStrings()
            .stream().map(HeaderTags.Version::new).collect(Collectors.toList());
    }

    /**
     * Get the obsolete flags header.
     * @return Value of header tag OBSOLETEFLAGS.
     */
    public List<Optional<String>> obsoletesFlags() {
        // @checkstyle MagicNumberCheck (2 lines)
        return Arrays.stream(this.meta.header(Header.HeaderTag.OBSOLETEFLAGS).asInts())
            .map(flag -> flag & 0xf)
            .mapToObj(Flags::find).collect(Collectors.toList());
    }

    /**
     * Get the conflicts name header.
     * @return Value of header tag CONFLICTNAME.
     */
    public List<String> conflicts() {
        return this.meta.header(Header.HeaderTag.CONFLICTNAME).asStrings();
    }

    /**
     * Get the conflicts versions header.
     * @return Value of header tag CONFLICTVERSION.
     */
    public List<HeaderTags.Version> conflictsVer() {
        return this.meta.header(Header.HeaderTag.CONFLICTVERSION).asStrings()
            .stream().map(HeaderTags.Version::new).collect(Collectors.toList());
    }

    /**
     * Get the conflicts flags header.
     * @return Value of header tag CONFLICTFLAGS.
     */
    public List<Optional<String>> conflictsFlags() {
        // @checkstyle MagicNumberCheck (2 lines)
        return Arrays.stream(this.meta.header(Header.HeaderTag.CONFLICTFLAGS).asInts())
            .map(flag -> flag & 0xf)
            .mapToObj(Flags::find).collect(Collectors.toList());
    }

    /**
     * Get the require flags headers as ints.
     * @return Value of header tag REQUIREFLAGS.
     */
    public List<Integer> requireFlagsInts() {
        final int[] array = this.meta.header(Header.HeaderTag.REQUIREFLAGS).asInts();
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

    /**
     * Get the base names header.
     * @return Value of header tag BASENAMES.
     */
    public List<String> baseNames() {
        return this.meta.header(Header.HeaderTag.BASENAMES).asStrings();
    }

    /**
     * Get the dir names header.
     * @return Value of header tag DIRNAMES.
     */
    public List<String> dirNames() {
        return this.meta.header(Header.HeaderTag.DIRNAMES).asStrings();
    }

    /**
     * Get the dir indexes header.
     * @return Value of header tag DIRINDEXES.
     */
    public int[] dirIndexes() {
        return this.meta.header(Header.HeaderTag.DIRINDEXES).asInts();
    }

    /**
     * Get the file modes header.
     * @return Value of header tag FILEMODES.
     */
    public int[] fileModes() {
        return this.meta.header(Header.HeaderTag.FILEMODES).asInts();
    }

    /**
     * Get the file flags header.
     * @return Value of header tag FILEFLAGS.
     */
    public int[] fileFlags() {
        return this.meta.header(Header.HeaderTag.FILEFLAGS).asInts();
    }

    /**
     * Get the changelog header.
     * @return Value of header tag CHANGELOG.
     */
    public List<String> changelog() {
        return this.meta.header(Header.HeaderTag.CHANGELOG).asStrings();
    }

    /**
     * Rpm package version, format is [epoch]:[version]-[release].
     * Comparison in implemented by first comparing epoch values as integer and
     * then comparing the rest part with {@link ComparableVersion}.
     * @since 1.9
     */
    public static final class Version implements Comparable<Version> {

        /**
         * Version format pattern.
         */
        private static final Pattern PTRN =
            Pattern.compile("((?<epoch>\\d+):)?(?<ver>[^/-]+|^(?!.))(-(?<rel>[^/]*))?");

        /**
         * Value from version header.
         */
        private final String val;

        /**
         * Ctor.
         * @param val Value from version header, can be empty
         */
        public Version(final String val) {
            this.val = val;
        }

        /**
         * Return version value.
         * @return String version
         */
        public String ver() {
            return this.part("ver").orElseThrow(
                () -> new ArtipieException(new IllegalArgumentException("Invalid version value"))
            );
        }

        /**
         * Release value.
         * @return String release, empty if not present
         */
        public Optional<String> rel() {
            return this.part("rel");
        }

        /**
         * Epoch value or default 0.
         * @return String epoch
         */
        public String epoch() {
            return this.part("epoch").orElse("0");
        }

        @Override
        public String toString() {
            return this.val;
        }

        @Override
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public int compareTo(final Version another) {
            int res;
            if (this.val.equals(another.val)) {
                res = 0;
            } else {
                res = Integer.compare(
                    Integer.parseInt(this.epoch()), Integer.parseInt(another.epoch())
                );
                if (res == 0) {
                    res = new ComparableVersion(
                        this.rel().map(rel -> String.format("%s-%s", this.ver(), rel))
                            .orElse(this.ver())
                    ).compareTo(
                        new ComparableVersion(
                            another.rel().map(rel -> String.format("%s-%s", another.ver(), rel))
                                .orElse(another.ver())
                        )
                    );
                }
            }
            return res;
        }

        /**
         * Get version part by name.
         * @param name Part group name, see {@link Version#PTRN}
         * @return Part value if found
         * @throws IllegalArgumentException If does not match
         */
        private Optional<String> part(final String name) {
            final Matcher matcher = Version.PTRN.matcher(this.val);
            if (matcher.matches()) {
                return Optional.ofNullable(matcher.group(name));
            }
            throw new ArtipieException(new IllegalArgumentException("Provided version is invalid"));
        }
    }

    /**
     * Rpm package dependency flags.
     * @since 1.10
     * @checkstyle JavadocVariableCheck (10 lines)
     */
    public enum Flags {
        EQUAL(8, "EQ"),
        GREATER(4, "GT"),
        LESS(2, "LT"),
        GREATER_OR_EQUAL(12, "GE"),
        LESS_OR_EQUAL(10, "LE");

        /**
         * Flag integer code.
         */
        private final int icode;

        /**
         * Flag short name.
         */
        private final String name;

        /**
         * Ctor.
         * @param icode Flag integer code
         * @param name Flag short name
         */
        Flags(final int icode, final String name) {
            this.icode = icode;
            this.name = name;
        }

        /**
         * Flag notation, short name.
         * @return String notation
         */
        public String notation() {
            return this.name;
        }

        /**
         * Flag integer code.
         * @return Flag code
         */
        public int code() {
            return this.icode;
        }

        /**
         * Find flag by code and return notation if found.
         * @param code Code to search for
         * @return Notation
         */
        static Optional<String> find(final int code) {
            return Arrays.stream(Flags.values()).filter(item -> code == item.icode)
                .findFirst().map(Flags::notation);
        }
    }
}
