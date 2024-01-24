/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.JsonObject;

/**
 * Merges provided metadata list with existing repodata.json.
 * @since 0.2
 */
public interface MergedJson {

    /**
     * Appends provided metadata to existing repodata.json.
     * @param items Items to add, filename <-> metadata json
     * @throws IOException On IO error
     */
    void merge(Map<String, JsonObject> items) throws IOException;

    /**
     * Implementation of {@link MergedJson} based on {@link com.fasterxml.jackson}.
     * @since 0.2
     */
    final class Jackson implements MergedJson {

        /**
         * Json object name `packages`.
         */
        private static final String PACKAGES = "packages";

        /**
         * Tar packages extension.
         */
        private static final String TAR = ".tar.bz2";

        /**
         * Json object name `packages.conda`.
         */
        private static final String PACKAGES_CONDA = "packages.conda";

        /**
         * Conda packages extension.
         */
        private static final String CONDA = ".conda";

        /**
         * Json generator.
         */
        private final JsonGenerator gnrt;

        /**
         * Json parser.
         */
        private final Optional<JsonParser> parser;

        /**
         * Ctor.
         * @param gnrt Json generator
         * @param parser Json parser
         */
        public Jackson(final JsonGenerator gnrt, final Optional<JsonParser> parser) {
            this.gnrt = gnrt;
            this.parser = parser;
        }

        @Override
        @SuppressWarnings({"PMD.AssignmentInOperand", "PMD.CognitiveComplexity"})
        public void merge(final Map<String, JsonObject> items) throws IOException {
            if (this.parser.isPresent()) {
                final AtomicReference<Boolean> tars;
                final AtomicReference<Boolean> condas;
                try (JsonParser prsr = this.parser.get()) {
                    JsonToken token;
                    tars = new AtomicReference<>(false);
                    condas = new AtomicReference<>(false);
                    while ((token = prsr.nextToken()) != null) {
                        if (token == JsonToken.END_OBJECT) {
                            if ((token = prsr.nextToken()) != null && token != JsonToken.END_OBJECT) {
                                this.gnrt.writeEndObject();
                                this.processJsonToken(items, prsr, token, tars, condas);
                            }
                        } else {
                            this.processJsonToken(items, prsr, token, tars, condas);
                        }
                    }
                }
                if (tars.get() ^ condas.get()) {
                    this.gnrt.writeEndObject();
                }
                if (!tars.get()) {
                    this.writePackagesItem(items, Jackson.PACKAGES, Jackson.TAR);
                }
                if (!condas.get()) {
                    this.writePackagesItem(items, Jackson.PACKAGES_CONDA, Jackson.CONDA);
                }
            } else {
                this.gnrt.writeStartObject();
                this.writePackagesItem(items, Jackson.PACKAGES, Jackson.TAR);
                this.writePackagesItem(items, Jackson.PACKAGES_CONDA, Jackson.CONDA);
            }
            this.gnrt.close();
        }

        /**
         * Processes current json token.
         * @param items Packages items to append
         * @param prsr Json parser
         * @param token Current token
         * @param tars Is it json object with .tar.bz2 items?
         * @param condas Is it json object with .conda items?
         * @throws IOException On IO error
                 */
        private void processJsonToken(final Map<String, JsonObject> items, final JsonParser prsr,
            final JsonToken token, final AtomicReference<Boolean> tars,
            final AtomicReference<Boolean> condas) throws IOException {
            if (token == JsonToken.FIELD_NAME && Jackson.PACKAGES.equals(prsr.getCurrentName())) {
                this.appendNewPackages(items, prsr, Jackson.TAR);
                tars.set(true);
            } else if (token == JsonToken.FIELD_NAME
                && Jackson.PACKAGES_CONDA.equals(prsr.getCurrentName())) {
                this.appendNewPackages(items, prsr, Jackson.CONDA);
                condas.set(true);
            } else if (token == JsonToken.FIELD_NAME
                && (prsr.getCurrentName().endsWith(Jackson.TAR)
                || prsr.getCurrentName().endsWith(Jackson.CONDA))) {
                final String name = prsr.getCurrentName();
                prsr.nextToken();
                prsr.setCodec(new ObjectMapper());
                final ObjectNode nodes = prsr.<ObjectNode>readValueAsTree();
                if (!items.containsKey(name)) {
                    this.gnrt.writeFieldName(name);
                    this.gnrt.setCodec(new ObjectMapper());
                    this.gnrt.writeTree(nodes);
                }
            } else {
                this.gnrt.copyCurrentEvent(prsr);
            }
        }

        /**
         * Writes `packages` or `packages.conda` json object element with the list of the new
         * packages.
         * @param items Packages to write
         * @param name Json object name: `packages` or `packages.conda`
         * @param type Packages type to write
         * @throws IOException On IO error
         */
        private void writePackagesItem(final Map<String, JsonObject> items,
            final String name, final String type) throws IOException {
            this.gnrt.writeFieldName(name);
            this.gnrt.writeStartObject();
            this.writeNewPackages(items, type);
            this.gnrt.writeEndObject();
        }

        /**
         * Appends new packages to existing repodata.json.
         * @param items Items to add
         * @param prsr Json parser
         * @param type Packages type
         * @throws IOException On IO error
         */
        private void appendNewPackages(final Map<String, JsonObject> items,
            final JsonParser prsr, final String type) throws IOException {
            this.gnrt.copyCurrentEvent(prsr);
            prsr.nextToken();
            this.gnrt.copyCurrentEvent(prsr);
            this.writeNewPackages(items, type);
        }

        /**
         * Writes new packages (.tar.bz2 or .conda) to json generator.
         * @param items Items to write
         * @param type Packages type
         * @throws IOException On IO error
         */
        private void writeNewPackages(final Map<String, JsonObject> items, final String type)
            throws IOException {
            for (final String pckg : items.keySet()) {
                if (pckg.endsWith(type)) {
                    this.gnrt.writeFieldName(pckg);
                    this.gnrt.setCodec(new ObjectMapper());
                    this.gnrt.writeTree(new ObjectMapper().readTree(items.get(pckg).toString()));
                }
            }
        }
    }
}
