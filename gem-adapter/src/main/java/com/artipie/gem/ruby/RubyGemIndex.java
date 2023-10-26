/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem.ruby;

import com.artipie.asto.ArtipieIOException;
import com.artipie.gem.GemIndex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Ruby runtime gem index implementation.
 *
 * @since 1.0
 */
public final class RubyGemIndex implements GemIndex, SharedRuntime.RubyPlugin {

    /**
     * Ruby runtime.
     */
    private final Ruby ruby;

    /**
     * New gem indexer.
     * @param ruby Runtime
     */
    public RubyGemIndex(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public void update(final Path path) {
        JavaEmbedUtils.invokeMethod(
            this.ruby,
            JavaEmbedUtils.newRuntimeAdapter().eval(this.ruby, "MetaRunner"),
            "new",
            new Object[]{path.toString()},
            Object.class
        );
    }

    @Override
    public String identifier() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void initialize() {
        final String script;
        try {
            script = IOUtils.toString(
                RubyGemIndex.class.getResourceAsStream("/metarunner.rb"),
                StandardCharsets.UTF_8
            );
        } catch (final IOException err) {
            throw new ArtipieIOException("Failed to initialize gem indexer", err);
        }
        JavaEmbedUtils.newRuntimeAdapter().eval(this.ruby, script);
    }
}
