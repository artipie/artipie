/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.files;

import com.jcabi.log.Logger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputStreamTo;
import org.cactoos.io.TeeInputStream;
import org.cactoos.scalar.LengthOf;

/**
 * Test bundle with RPM packages.
 * @since 0.8
 */
public final class TestBundle {

    /**
     * Size of the bundle.
     */
    private final URL url;

    /**
     * Ctor.
     * @param url URL
     */
    public TestBundle(final URL url) {
        this.url = url;
    }

    /**
     * Ctor.
     * @param size Bundle size
     */
    public TestBundle(final Size size) {
        this(size.url());
    }

    /**
     * Unpack bundle to path.
     * @param path Destination path
     * @return Bundle archive file
     * @throws IOException On error
     */
    public Path load(final Path path) throws IOException {
        final String[] parts = this.url.getPath().split("/");
        final String name = parts[parts.length - 1];
        final Path bundle = path.resolve(name);
        final long start = System.currentTimeMillis();
        Logger.info(this, "Loading bundle %s from %s to %s", name, this.url, bundle);
        try (TeeInputStream tee =
            new TeeInputStream(
                new BufferedInputStream(this.url.openStream()),
                new OutputStreamTo(bundle)
            )
        ) {
            new LengthOf(new InputOf(tee)).intValue();
        }
        if (Logger.isInfoEnabled(this)) {
            Logger.info(
                this,
                "Downloaded bundle %s in %[ms]s",
                name, System.currentTimeMillis() - start
            );
        }
        return bundle;
    }

    /**
     * Bundle size.
     */
    public enum Size {

        /**
         * Hundred rpms bundle.
         */
        HUNDRED(100),

        /**
         * Thousand rpms bundle.
         */
        THOUSAND(1000);

        /**
         * Test bundle size.
         */
        private final int cnt;

        /**
         * Ctor.
         * @param count Rpm packages count
         */
        Size(final int count) {
            this.cnt = count;
        }

        /**
         * Bundle file name without extension.
         * @return Name
         */
        public String filename() {
            return String.format("bundle%d", this.cnt);
        }

        /**
         * Rpm packages in bundle count.
         * @return Int count
         */
        public int count() {
            return this.cnt;
        }

        /**
         * Returns ULR instance.
         * @return Url
         */
        URL url() {
            try {
                return URI.create(
                    String.format(
                        "https://artipie.s3.amazonaws.com/rpm-test/%s.tar.gz", this.filename()
                    )
                ).toURL();
            } catch (final MalformedURLException ex) {
                throw new IllegalArgumentException("Invalid url", ex);
            }
        }
    }
}
