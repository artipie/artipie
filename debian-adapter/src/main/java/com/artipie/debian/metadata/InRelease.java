/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.debian.Config;
import com.artipie.debian.GpgConfig;
import com.artipie.debian.misc.GpgClearsign;
import java.util.concurrent.CompletionStage;

/**
 * InRelease index file.
 * Check the <a href="https://wiki.debian.org/DebianRepository/Format#A.22Release.22_files">docs</a>
 * for more information.
 * @since 0.4
 */
public interface InRelease {

    /**
     * Generates InRelease index file by provided Release index.
     * @param release Release index key
     * @return Completion action
     */
    CompletionStage<Void> generate(Key release);

    /**
     * Key (storage item key) of the InRelease index.
     * @return Storage item
     */
    Key key();

    /**
     * Implementation of {@link InRelease} from abstract storage.
     * @since 0.4
     */
    final class Asto implements InRelease {

        /**
         * Abstract storage.
         */
        private final Storage asto;

        /**
         * Repository config.
         */
        private final Config config;

        /**
         * Ctor.
         * @param asto Abstract storage
         * @param config Repository config
         */
        public Asto(final Storage asto, final Config config) {
            this.asto = asto;
            this.config = config;
        }

        @Override
        public CompletionStage<Void> generate(final Key release) {
            final CompletionStage<Void> res;
            if (this.config.gpg().isPresent()) {
                final GpgConfig gpg = this.config.gpg().get();
                res = this.asto.value(release).thenApply(PublisherAs::new)
                    .thenCompose(PublisherAs::bytes)
                    .thenCompose(
                        bytes -> gpg.key().thenApply(
                            key -> new GpgClearsign(bytes).signedContent(key, gpg.password())
                        )
                    ).thenCompose(bytes -> this.asto.save(this.key(), new Content.From(bytes)));
            } else {
                res = this.asto.value(release).thenCompose(
                    content -> this.asto.save(this.key(), content)
                );
            }
            return res;
        }

        @Override
        public Key key() {
            return new Key.From("dists", this.config.codename(), "InRelease");
        }
    }
}
