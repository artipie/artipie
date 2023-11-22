/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;

/**
 * Test rpm.
 * @since 0.9
 */
public interface TestRpm {

    /**
     * Puts test package to storage.
     * @param storage Storage
     * @throws IOException On error
     */
    void put(Storage storage) throws IOException;

    /**
     * Name of the rpm.
     * @return Name of the rpm
     */
    String name();

    /**
     * Rpm path.
     * @return Path
     */
    Path path();

    /**
     * Spell checker sentos rpm.
     * @since 1.4
     */
    final class Aspell extends FromPath {

        /**
         * Ctor.
         */
        public Aspell() {
            super("aspell-0.60.6.1-9.el7.x86_64.rpm");
        }
    }

    /**
     * Time sentos rpm.
     * @since 0.9
     */
    final class Time extends FromPath {

        /**
         * Ctor.
         */
        public Time() {
            super("time-1.7-45.el7.x86_64.rpm");
        }
    }

    /**
     * Abc test rpm.
     * @since 0.9
     */
    final class Abc extends FromPath {

        /**
         * Ctor.
         */
        public Abc() {
            super("abc-1.01-26.git20200127.fc32.ppc64le.rpm");
        }

        /**
         * Rpm metadata path.
         * @param type Xml package type
         * @return Path
         * @checkstyle NonStaticMethodCheck (5 line)
         */
        public Path metadata(final XmlPackage type) {
            return new TestResource(
                String.format("repodata/abc-%s.xml.example", type.lowercase())
            ).asPath();
        }
    }

    /**
     * Libdeflt test rpm.
     * @since 0.9
     */
    final class Libdeflt extends FromPath {

        /**
         * Ctor.
         */
        public Libdeflt() {
            super("libdeflt1_0-2020.03.27-25.1.armv7hl.rpm");
        }

        /**
         * Rpm metadata path.
         * @param type Xml package type
         * @return Path
         * @checkstyle NonStaticMethodCheck (5 line)
         */
        public Path metadata(final XmlPackage type) {
            return new TestResource(
                String.format("repodata/libdeflt-%s.xml.example", type.lowercase())
            ).asPath();
        }
    }

    /**
     * Abstract from file implementation.
     * @since 0.9
     */
    abstract class FromPath implements TestRpm {

        /**
         * Origin.
         */
        private final Path path;

        /**
         * Ctor.
         * @param file Rpm file name
         */
        protected FromPath(final String file) {
            this(new TestResource(file).asPath());
        }

        /**
         * Primary ctor.
         * @param path Rpm file path
         */
        private FromPath(final Path path) {
            this.path = path;
        }

        @Override
        public final void put(final Storage storage) throws IOException {
            storage.save(
                new Key.From(this.path.getFileName().toString()),
                new Content.From(Files.readAllBytes(this.path))
            ).join();
        }

        @Override
        public final String name() {
            return this.path.getFileName().toString().replaceAll("\\.rpm$", "");
        }

        @Override
        public final Path path() {
            return this.path;
        }
    }

    /**
     * An invalid rpm.
     * @since 0.9
     */
    final class Invalid implements TestRpm {

        /**
         * Invalid bytes content.
         */
        private final byte[] content = new byte[] {0x00, 0x01, 0x02 };

        @Override
        public void put(final Storage storage) {
            storage.save(
                new Key.From(String.format("%s.rpm", this.name())),
                new Content.From(this.content)
            ).join();
        }

        @Override
        public String name() {
            return "invalid";
        }

        @Override
        public Path path() {
            throw new UnsupportedOperationException(
                "Path is not available for invalid rpm package"
            );
        }

        /**
         * Bytes representation.
         * @return Invalid bytes content
         */
        public byte[] bytes() {
            return this.content;
        }

    }

    /**
     * Multiple test rpms.
     * @since 0.9
     */
    final class Multiple {

        /**
         * Rpms.
         */
        private final Iterable<TestRpm> rpms;

        /**
         * Ctor.
         * @param rpms Rpms.
         */
        public Multiple(final TestRpm... rpms) {
            this(new ListOf<>(rpms));
        }

        /**
         * Ctor.
         * @param rpms Rpms.
         */
        public Multiple(final Iterable<TestRpm> rpms) {
            this.rpms = rpms;
        }

        /**
         * Put rpms into storage.
         * @param storage Storage
         * @throws IOException On error
         */
        public void put(final Storage storage) throws IOException {
            for (final TestRpm rpm: this.rpms) {
                rpm.put(storage);
            }
        }

    }
}
