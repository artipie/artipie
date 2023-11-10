/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Set;

/**
 * Json maid removes items from json repodata.
 * @since 0.1
 */
public interface JsonMaid {

    /**
     * Cleans xml by ids (checksums) and returns actual package count.
     * @param checksums Checksums
     * @throws IOException When something wrong
     */
    void clean(Set<String> checksums) throws IOException;

    /**
     * Implementation of {@link JsonMaid} based on {@link com.fasterxml.jackson}.
     * @since 0.1
     */
    final class Jackson implements JsonMaid {

        /**
         * Json generator.
         */
        private final JsonGenerator gnrt;

        /**
         * Json parser.
         */
        private final JsonParser parser;

        /**
         * Ctor.
         * @param gnrt Json generator
         * @param parser Json parser
         */
        public Jackson(final JsonGenerator gnrt, final JsonParser parser) {
            this.gnrt = gnrt;
            this.parser = parser;
        }

        @Override
        @SuppressWarnings("PMD.AssignmentInOperand")
        public void clean(final Set<String> checksums) throws IOException {
            JsonToken token;
            while ((token = this.parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME
                    && !"packages.conda".equals(this.parser.getCurrentName())
                    && (this.parser.getCurrentName().endsWith("tar.bz2")
                        || this.parser.getCurrentName().endsWith(".conda"))) {
                    final String name = this.parser.getCurrentName();
                    this.parser.nextToken();
                    this.parser.setCodec(new ObjectMapper());
                    final ObjectNode nodes = this.parser.<ObjectNode>readValueAsTree();
                    if (!checksums.contains(nodes.get("sha256").asText())) {
                        this.gnrt.writeFieldName(name);
                        this.gnrt.setCodec(new ObjectMapper());
                        this.gnrt.writeTree(nodes);
                    }
                } else {
                    this.gnrt.copyCurrentEvent(this.parser);
                }
            }
            this.gnrt.close();
            this.parser.close();
        }
    }
}
