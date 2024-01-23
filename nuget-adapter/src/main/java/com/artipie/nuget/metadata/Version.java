/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget.metadata;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version of package.
 * See <a href="https://docs.microsoft.com/en-us/nuget/concepts/package-versioning">Package versioning</a>.
 * See <a href="https://docs.microsoft.com/en-us/nuget/concepts/package-versioning#normalized-version-numbers">Normalized version numbers</a>.
 * Comparison of version strings is implemented using SemVer 2.0.0's <a href="https://semver.org/spec/v2.0.0.html#spec-item-11">version precedence rules</a>.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class Version implements Comparable<Version>, NuspecField {

    /**
     * RegEx pattern for matching version string.
     *
         */
    private static final Pattern PATTERN = Pattern.compile(
        String.join(
            "",
            "(?<major>\\d+)\\.(?<minor>\\d+)",
            "(\\.(?<patch>\\d+)(\\.(?<revision>\\d+))?)?",
            "(-(?<label>[0-9a-zA-Z\\-]+(\\.[0-9a-zA-Z\\-]+)*))?",
            "(\\+(?<metadata>[0-9a-zA-Z\\-]+(\\.[0-9a-zA-Z\\-]+)*))?",
            "$"
        )
    );

    /**
     * Raw value of version tag.
     */
    private final String val;

    /**
     * Ctor.
     *
     * @param raw Raw value of version tag.
     */
    public Version(final String raw) {
        this.val = raw;
    }

    @Override
    public String raw() {
        return this.val;
    }

    @Override
    public String normalized() {
        final StringBuilder builder = new StringBuilder()
            .append(removeLeadingZeroes(this.major()))
            .append('.')
            .append(removeLeadingZeroes(this.minor()));
        this.patch().ifPresent(
            patch -> builder.append('.').append(removeLeadingZeroes(patch))
        );
        this.revision().ifPresent(
            revision -> {
                final String rev = removeLeadingZeroes(revision);
                if (!"0".equals(rev)) {
                    builder.append('.').append(rev);
                }
            }
        );
        this.label().ifPresent(
            label -> builder.append('-').append(label)
        );
        return builder.toString();
    }

    @Override
    public int compareTo(final Version that) {
        return Comparator
            .<Version>comparingInt(version -> Integer.parseInt(version.major()))
            .thenComparingInt(version -> Integer.parseInt(version.minor()))
            .thenComparingInt(version -> version.patch().map(Integer::parseInt).orElse(0))
            .thenComparingInt(version -> version.revision().map(Integer::parseInt).orElse(0))
            .thenComparing(Version::compareLabelTo)
            .compare(this, that);
    }

    @Override
    public String toString() {
        return this.val;
    }

    /**
     * Is the version compliant to sem ver 2.0.0? Returns true if either of the following
     * statements is true:
     * a) The pre-release label is dot-separated, for example, 1.0.0-alpha.1
     * b) The version has build-metadata, for example, 1.0.0+githash
     * Based on the NuGet <a href="https://docs.microsoft.com/en-us/nuget/concepts/package-versioning#semantic-versioning-200">documentation</a>.
     * @return True if version is sem ver 2.0.0
     */
    public boolean isSemVerTwo() {
        return this.metadata().isPresent()
            || this.label().map(lbl -> lbl.contains(".")).orElse(false);
    }

    /**
     * Is this a pre-pelease version?
     * @return True if contains pre-release label
     */
    public boolean isPrerelease() {
        return this.label().isPresent();
    }

    /**
     * Major version.
     *
     * @return String representation of major version.
     */
    private String major() {
        return this.group("major").orElseThrow(
            () -> new IllegalStateException("Major identifier is missing")
        );
    }

    /**
     * Minor version.
     *
     * @return String representation of minor version.
     */
    private String minor() {
        return this.group("minor").orElseThrow(
            () -> new IllegalStateException("Minor identifier is missing")
        );
    }

    /**
     * Patch part of version.
     *
     * @return Patch part of version, none if absent.
     */
    private Optional<String> patch() {
        return this.group("patch");
    }

    /**
     * Revision part of version.
     *
     * @return Revision part of version, none if absent.
     */
    private Optional<String> revision() {
        return this.group("revision");
    }

    /**
     * Label part of version.
     *
     * @return Label part of version, none if absent.
     */
    private Optional<String> label() {
        return this.group("label");
    }

    /**
     * Metadata part of version.
     *
     * @return Metadata part of version, none if absent.
     */
    private Optional<String> metadata() {
        return this.group("metadata");
    }

    /**
     * Get named group from RegEx matcher.
     *
     * @param name Group name.
     * @return Group value, or nothing if absent.
     */
    private Optional<String> group(final String name) {
        return Optional.ofNullable(this.matcher().group(name));
    }

    /**
     * Get RegEx matcher by version pattern.
     *
     * @return Matcher by pattern.
     */
    private Matcher matcher() {
        final Matcher matcher = PATTERN.matcher(this.val);
        if (!matcher.find()) {
            throw new IllegalStateException(
                String.format("Unexpected version format: %s", this.val)
            );
        }
        return matcher;
    }

    /**
     * Compares labels with other version.
     *
     * @param that Other version to compare.
     * @return Comparison result, by rules of {@link Comparable#compareTo(Object)}
     */
    private int compareLabelTo(final Version that) {
        final Optional<String> one = this.label();
        final Optional<String> two = that.label();
        final int result;
        if (one.isPresent()) {
            if (two.isPresent()) {
                result = Comparator
                    .comparing(VersionLabel::new)
                    .compare(one.get(), two.get());
            } else {
                result = -1;
            }
        } else {
            if (two.isPresent()) {
                result = 1;
            } else {
                result = 0;
            }
        }
        return result;
    }

    /**
     * Removes leading zeroes from a string. Last zero is preserved.
     *
     * @param string Original string.
     * @return String without leading zeroes.
     */
    private static String removeLeadingZeroes(final String string) {
        return string.replaceFirst("^0+(?!$)", "");
    }
}
