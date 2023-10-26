/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.metadata;

import com.artipie.helm.misc.DateTimeNow;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

/**
 * Yaml writer with specified indent.
 * @since 0.3
 */
public final class YamlWriter {
    /**
     * Generated tag.
     */
    static final String TAG_GENERATED = "generated:";

    /**
     * Required indent.
     */
    private final int indnt;

    /**
     * Buffered writer.
     */
    private final BufferedWriter writer;

    /**
     * Ctor.
     * @param writer Writer
     * @param indent Required indent in index file
     */
    public YamlWriter(final BufferedWriter writer, final int indent) {
        this.writer = writer;
        this.indnt = indent;
    }

    /**
     * Obtains indent.
     * @return Indent.
     */
    public int indent() {
        return this.indnt;
    }

    /**
     * Write data and a new line.
     * @param data Data which should be written
     * @param xindendt How many times the minimum value of indent should be increased?
     * @throws IOException In case of error during writing.
     */
    public void writeLine(final String data, final int xindendt) throws IOException {
        final StringBuilder bldr = new StringBuilder();
        this.writer.write(
            bldr.append(StringUtils.repeat(' ', xindendt * this.indnt))
                .append(data).toString()
        );
        this.writer.newLine();
    }

    /**
     * Write line if it does not start with tag generated. Otherwise replaces the value
     * of tag `generated` to update time when this index was generated.
     * @param line Parsed line
     * @throws IOException In case of exception during writing
     */
    public void writeAndReplaceTagGenerated(final String line) throws IOException {
        if (line.startsWith(YamlWriter.TAG_GENERATED)) {
            final StringBuilder bldr = new StringBuilder();
            this.writeLine(
                bldr.append(YamlWriter.TAG_GENERATED)
                    .append(" ")
                    .append(new DateTimeNow().asString())
                    .toString(),
                0
            );
        } else {
            this.writeLine(line, 0);
        }
    }
}
