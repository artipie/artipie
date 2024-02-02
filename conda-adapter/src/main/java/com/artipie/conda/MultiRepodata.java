/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.ArtipieIOException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Multi conda repodata: merges repodata (possibly obtained from different remotes)
 * into single repodata index.
 * @since 0.3
 */
public interface MultiRepodata {

    /**
     * Merges repodata.jsons into single repodata.json.
     * @param inputs Collections of repodata.json to merge
     * @param result Where to write the result
     */
    void merge(Collection<InputStream> inputs, OutputStream result);

    /**
     * Implementation of {@link MultiRepodata} that merges Repodata.json indexes checking for
     * duplicates and writes unique `packages` and `packages.conda` to the output stream.
     * Duplicates are checked by filename, first met package is written into resulting repodata,
     * other packages with the same filename are skipped.
     * Implementation does not close input or output streams, these operations should be made from
     * the outside.
     * @since 0.3
     */
    @SuppressWarnings("PMD.CloseResource")
    final class Unique implements MultiRepodata {

        /**
         * Temp file extension.
         */
        private static final String EXT = "json";

        /**
         * Repodata.json field name "packages.conda".
         */
        private static final String FIELD = "packages.conda";

        /**
         * Filenames of the packages.
         */
        private final Set<String> pckgs = new HashSet<>();

        @Override
        public void merge(final Collection<InputStream> inputs, final OutputStream result) {
            final JsonFactory factory = new JsonFactory();
            try {
                final Path ftars = Files.createTempFile("tars", Unique.EXT);
                final Path fcondas = Files.createTempFile("condas", Unique.EXT);
                try {
                    try (
                        OutputStream otars = new BufferedOutputStream(Files.newOutputStream(ftars));
                        OutputStream ocondas =
                            new BufferedOutputStream(Files.newOutputStream(fcondas))
                    ) {
                        final JsonGenerator tars = factory.createGenerator(otars);
                        final JsonGenerator condas = factory.createGenerator(ocondas);
                        tars.writeStartObject();
                        condas.writeStartObject();
                        for (final InputStream item : inputs) {
                            this.processInput(factory.createParser(item), tars, condas);
                        }
                        tars.close();
                        condas.close();
                    }
                    try (
                        InputStream itars = new BufferedInputStream(Files.newInputStream(ftars));
                        InputStream icondas = new BufferedInputStream(Files.newInputStream(fcondas))
                    ) {
                        final JsonGenerator res = factory.createGenerator(result);
                        res.writeStartObject();
                        Unique.writePackages(factory.createParser(itars), res, "packages");
                        Unique.writePackages(factory.createParser(icondas), res, Unique.FIELD);
                        res.writeEndObject();
                        res.close();
                    }
                } finally {
                    Files.delete(ftars);
                    Files.delete(fcondas);
                }
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }

        /**
         * Processes input (packages.json) by writing unique packages info into temp
         * outputs.
         * @param parser Parser input
         * @param tars Output for tars packages
         * @param condas Output for condas packages
         * @throws IOException On IO error
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private void processInput(final JsonParser parser, final JsonGenerator tars,
            final JsonGenerator condas) throws IOException {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME
                    && !Unique.FIELD.equals(parser.getCurrentName())
                    && parser.getCurrentName().endsWith(".conda")) {
                    this.writeItem(parser, condas);
                } else if (token == JsonToken.FIELD_NAME
                    && parser.getCurrentName().endsWith(".tar.bz2")) {
                    this.writeItem(parser, tars);
                }
            }
        }

        /**
         * Writes package item from parser to the resulting output (JsonGenerator).
         * @param parser Where to read from
         * @param generator Where to write
         * @throws IOException On IO error
         */
        private void writeItem(final JsonParser parser, final JsonGenerator generator)
            throws IOException {
            final String name = parser.getCurrentName();
            parser.nextToken();
            parser.setCodec(new ObjectMapper());
            final ObjectNode nodes = parser.<ObjectNode>readValueAsTree();
            if (this.pckgs.add(name)) {
                generator.writeFieldName(name);
                generator.setCodec(new ObjectMapper());
                generator.writeTree(nodes);
            }
        }

        /**
         * Writes pacckages to the resulting file.
         * @param parser Parser to write and parse
         * @param res Where to write the result
         * @param field Field name
         * @throws IOException On IO error
         */
        private static void writePackages(final JsonParser parser, final JsonGenerator res,
            final String field) throws IOException {
            res.writeFieldName(field);
            while (parser.nextToken() != null) {
                res.copyCurrentEvent(parser);
            }
        }
    }
}
