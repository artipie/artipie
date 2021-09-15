/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Testcontainers logger consumer buffer.
 * @since 0.23
 */
final class ContainerLogger implements Consumer<OutputFrame> {

    /**
     * Buffer.
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder buffer;

    /**
     * New logger.
     */
    ContainerLogger() {
        this(new StringBuilder());
    }

    /**
     * New logger with buffer.
     * @param buffer String builder
     */
    ContainerLogger(final StringBuilder buffer) {
        this.buffer = buffer;
    }

    @Override
    public void accept(final OutputFrame fram) {
        this.buffer.append(fram.getUtf8String());
    }

    @Override
    public String toString() {
        return this.buffer.toString();
    }
}
