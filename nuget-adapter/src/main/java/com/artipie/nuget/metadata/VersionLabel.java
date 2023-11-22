/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget.metadata;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Label part of version.
 * See <a href="https://semver.org/spec/v2.0.0.html#spec-item-9">https://semver.org/spec/v2.0.0.html#spec-item-9</a>.
 *
 * @since 0.1
 */
final class VersionLabel implements Comparable<VersionLabel> {

    /**
     * Version label string.
     */
    private final String label;

    /**
     * Ctor.
     *
     * @param label Version label string.
     */
    VersionLabel(final String label) {
        this.label = label;
    }

    @Override
    public int compareTo(final VersionLabel that) {
        final List<Identifier> one = this.identifiers();
        final List<Identifier> two = that.identifiers();
        int compare = 0;
        for (int index = 0; index < one.size(); index += 1) {
            if (index >= two.size()) {
                compare = 1;
                break;
            }
            final int result = one.get(index).compareTo(two.get(index));
            if (result != 0) {
                compare = result;
                break;
            }
        }
        if (compare == 0 && one.size() < two.size()) {
            compare = -1;
        }
        return compare;
    }

    /**
     * Ordered sequence of identifiers representing this label.
     *
     * @return List of identifiers.
     */
    private List<Identifier> identifiers() {
        return Stream.of(this.label.split("\\."))
            .map(Identifier::new)
            .collect(Collectors.toList());
    }

    /**
     * Identifier, part of label.
     *
     * @since 0.1
     */
    private static class Identifier implements Comparable<Identifier> {

        /**
         * String representation.
         */
        private final String value;

        /**
         * Ctor.
         *
         * @param value Version label string.
         */
        Identifier(final String value) {
            this.value = value;
        }

        @Override
        public int compareTo(final Identifier that) {
            final OptionalInt one = this.number();
            final OptionalInt two = that.number();
            final int compare;
            if (one.isPresent()) {
                if (two.isPresent()) {
                    compare = Integer.compare(one.getAsInt(), two.getAsInt());
                } else {
                    compare = -1;
                }
            } else {
                if (two.isPresent()) {
                    compare = 1;
                } else {
                    compare = this.value.compareTo(that.value);
                }
            }
            return compare;
        }

        /**
         * Get numeric representation of identifier.
         *
         * @return Numeric value of identifier, empty if identifier contains some non-digits.
         */
        private OptionalInt number() {
            final OptionalInt res;
            if (this.value.matches("\\d+")) {
                res = OptionalInt.of(Integer.parseInt(this.value));
            } else {
                res = OptionalInt.empty();
            }
            return res;
        }
    }
}
