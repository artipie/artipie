/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.ruby;

import com.artipie.asto.ArtipieIOException;
import com.artipie.gem.GemDependencies;
import com.artipie.gem.ruby.SharedRuntime.RubyPlugin;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Gem dependencies JRuby implementation.
 * @since 1.3
 */
public final class RubyGemDependencies implements GemDependencies, RubyPlugin {

    /**
     * Ruby runtime.
     */
    private final Ruby ruby;

    /**
     * New dependencies provider.
     * @param ruby Ruby runtime.
     */
    public RubyGemDependencies(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public ByteBuffer dependencies(final Set<? extends Path> gems) {
        final String raw = JavaEmbedUtils.invokeMethod(
            this.ruby,
            JavaEmbedUtils.newRuntimeAdapter().eval(this.ruby, "Dependencies"),
            "dependencies",
            new Object[]{
                gems.stream().map(Path::toString)
                    .collect(Collectors.toList()).toArray(new String[0]),
            },
            String.class
        );
        return ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String identifier() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void initialize() {
        try {
            JavaEmbedUtils.newRuntimeAdapter().eval(
                this.ruby,
                IOUtils.toString(
                    this.getClass().getResourceAsStream("/dependencies.rb"),
                    StandardCharsets.UTF_8
                )
            );
        } catch (final IOException err) {
            throw new ArtipieIOException("Failed to load dependencies script", err);
        }
    }
}
