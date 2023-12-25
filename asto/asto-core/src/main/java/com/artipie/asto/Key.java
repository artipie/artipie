/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage key.
 *
 * @since 0.6
 */
public interface Key {
    /**
     * Delimiter used to split string into parts and join parts into string.
     */
    String DELIMITER = "/";

    /**
     * Comparator for key values by their string representation.
     */
    Comparator<Key> CMP_STRING = Comparator.comparing(Key::string);

    /**
     * Root key.
     */
    Key ROOT = new Key.From(Collections.emptyList());

    /**
     * Key.
     * @return Key string
     */
    String string();

    /**
     * Parent key.
     * @return Parent key or Optional.empty if the current key is ROOT
     */
    Optional<Key> parent();

    /**
     * Parts of key.
     * @return List of parts
     */
    List<String> parts();

    /**
     * Base key class.
     * @since 1.14.0
     */
    abstract class Base implements Key {
        @Override
        public boolean equals(final Object another) {
            if (this == another) {
                return true;
            }
            if (!(another instanceof Key)) {
                return false;
            }
            final Key from = (Key) another;
            return Objects.equals(this.parts(), from.parts());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.parts());
        }

        @Override
        public String toString() {
            return this.string();
        }
    }

    /**
     * Default decorator.
     * @since 0.7
     */
    abstract class Wrap extends Base {

        /**
         * Origin key.
         */
        private final Key origin;

        /**
         * Ctor.
         * @param origin Origin key
         */
        protected Wrap(final Key origin) {
            this.origin = origin;
        }

        @Override
        public final String string() {
            return this.origin.string();
        }

        @Override
        public Optional<Key> parent() {
            return this.origin.parent();
        }

        @Override
        public List<String> parts() {
            return this.origin.parts();
        }

        @Override
        public final String toString() {
            return this.string();
        }

        @Override
        public boolean equals(final Object another) {
            return this.origin.equals(another);
        }

        @Override
        public int hashCode() {
            return this.origin.hashCode();
        }
    }

    /**
     * Key from something.
     * @since 0.6
     */
    final class From extends Base {

        /**
         * Parts.
         */
        private final List<String> parts;

        /**
         * Ctor.
         * @param parts Parts delimited by `/` symbol
         */
        public From(final String parts) {
            this(parts.split(Key.DELIMITER));
        }

        /**
         * Ctor.
         * @param parts Parts
         */
        public From(final String... parts) {
            this(Arrays.asList(parts));
        }

        /**
         * Key from two keys.
         * @param first First key
         * @param second Second key
         */
        public From(final Key first, final Key second) {
            this(
                Stream.concat(
                    new From(first).parts.stream(),
                    new From(second).parts.stream()
                ).collect(Collectors.toList())
            );
        }

        /**
         * From base path and parts.
         * @param base Base path
         * @param parts Parts
         */
        public From(final Key base, final String... parts) {
            this(
                Stream.concat(
                    new From(base.string()).parts.stream(),
                    Arrays.stream(parts)
                ).collect(Collectors.toList())
            );
        }

        /**
         * Ctor.
         * @param parts Parts
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        public From(final List<String> parts) {
            if (parts.size() == 1 && parts.get(0).isEmpty()) {
                this.parts = Collections.emptyList();
            } else {
                this.parts = parts.stream()
                    .flatMap(part -> Arrays.stream(part.split("/")))
                    .collect(Collectors.toList());
            }
        }

        @Override
        public String string() {
            for (final String part : this.parts) {
                if (part.isEmpty()) {
                    throw new ArtipieException("Empty parts are not allowed");
                }
                if (part.contains(Key.DELIMITER)) {
                    throw new ArtipieException(String.format("Invalid part: '%s'", part));
                }
            }
            return String.join(Key.DELIMITER, this.parts);
        }

        @Override
        public Optional<Key> parent() {
            final Optional<Key> parent;
            if (this.parts.isEmpty()) {
                parent = Optional.empty();
            } else {
                parent = Optional.of(
                    new Key.From(this.parts.subList(0, this.parts.size() - 1))
                );
            }
            return parent;
        }

        @Override
        public List<String> parts() {
            return Collections.unmodifiableList(this.parts);
        }
    }

}
