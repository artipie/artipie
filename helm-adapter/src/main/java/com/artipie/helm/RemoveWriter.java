/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.helm.metadata.Index;
import com.artipie.helm.metadata.ParsedChartName;
import com.artipie.helm.metadata.YamlWriter;
import com.artipie.helm.misc.EmptyIndex;
import com.artipie.helm.misc.SpaceInBeginning;
import com.artipie.http.misc.TokenizerFlatProc;
import hu.akarnokd.rxjava2.interop.FlowableInterop;
import io.reactivex.Flowable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Remove writer of info about charts from index file.
 * @since 0.3
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 * @checkstyle NPathComplexityCheck (500 lines)
 */
public interface RemoveWriter {
    /**
     * Rewrites source index file avoiding writing down info about charts which
     * contains in charts collection. If passed for deletion chart does bot exist
     * in index file, an exception should be thrown. It processes source file by
     * reading batch of versions for each chart from source index. Then versions
     * which should not be deleted from file are rewritten to new index file.
     * @param source Key to source index file
     * @param out Path to temporary file in which new index would be written
     * @param todelete Collection with charts with specified versions which should be deleted
     * @return Result of completion
     */
    CompletionStage<Void> delete(Key source, Path out, Map<String, Set<String>> todelete);

    /**
     * Implementation of {@link RemoveWriter} for abstract storage.
     * @since 0.3
     */
    final class Asto implements RemoveWriter {
        /**
         * Versions.
         */
        static final String VRSNS = "version:";

        /**
         * Entries.
         */
        static final String ENTRS = "entries:";

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param storage Storage
         */
        Asto(final Storage storage) {
            this.storage = storage;
        }

        @Override
        @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts", "PMD.NPathComplexity"})
        public CompletionStage<Void> delete(
            final Key source,
            final Path out,
            final Map<String, Set<String>> todelete
        ) {
            return new Index.WithBreaks(this.storage)
                .versionsByPackages(source)
                .thenCompose(
                    fromidx -> {
                        checkExistenceChartsToDelete(fromidx, todelete);
                        return CompletableFuture.allOf();
                    }
                ).thenCompose(
                    noth ->  {
                        try {
                            final OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(out));
                            final BufferedWriter bufw = new BufferedWriter(osw);
                            final TokenizerFlatProc target = new TokenizerFlatProc("\n");
                            return this.contentOfIndex(source)
                                .thenAccept(cont -> cont.subscribe(target))
                                .thenCompose(
                                    none -> Flowable.fromPublisher(target)
                                        .map(buf -> new String(new Remaining(buf).bytes()))
                                        .scan(
                                            new ScanContext(bufw, 2),
                                            (ctx, curr) -> {
                                                final String trimmed = curr.trim();
                                                final int pos = new SpaceInBeginning(curr).last();
                                                if (!ctx.inentries) {
                                                    ctx.setEntries(trimmed.equals(Asto.ENTRS));
                                                }
                                                if (ctx.inentries
                                                    && new ParsedChartName(curr).valid()) {
                                                    if (ctx.name.isEmpty()) {
                                                        ctx.setWriter(new YamlWriter(bufw, pos));
                                                    }
                                                    if (pos == ctx.wrtr.indent()) {
                                                        if (!ctx.name.isEmpty()) {
                                                            writeIfNotContainInDeleted(
                                                                ctx.lines, todelete, ctx.wrtr
                                                            );
                                                        }
                                                        ctx.setName(trimmed.replace(":", ""));
                                                    }
                                                }
                                                if (ctx.inentries
                                                    && !ctx.name.isEmpty()
                                                    && pos == 0
                                                ) {
                                                    ctx.setEntries(false);
                                                    writeIfNotContainInDeleted(
                                                        ctx.lines, todelete, ctx.wrtr
                                                    );
                                                }
                                                if (ctx.inentries && !ctx.name.isEmpty()) {
                                                    ctx.addLine(curr);
                                                }
                                                if (ctx.lines.isEmpty()) {
                                                    ctx.wrtr.writeAndReplaceTagGenerated(curr);
                                                }
                                                return ctx;
                                            }
                                        ).to(FlowableInterop.last())
                                        .thenCompose(
                                            ctx -> {
                                                try {
                                                    bufw.close();
                                                    osw.close();
                                                } catch (final IOException exc) {
                                                    throw new ArtipieIOException(exc);
                                                }
                                                return CompletableFuture.allOf();
                                            }
                                        )
                                );
                        } catch (final IOException exc) {
                            throw new ArtipieIOException(exc);
                        }
                    }
                );
        }

        /**
         * Obtains content of index file by key or returns an empty index file.
         * @param source Key to source index file
         * @return Index file by key from storage or an empty index file.
         */
        private CompletionStage<Content> contentOfIndex(final Key source) {
            return this.storage.exists(source)
                .thenCompose(
                    exists -> {
                        final CompletionStage<Content> res;
                        if (exists) {
                            res = this.storage.value(source);
                        } else {
                            res = CompletableFuture.completedFuture(
                                new EmptyIndex().asContent()
                            );
                        }
                        return res;
                    }
                );
        }

        /**
         * Writes info about all versions of chart to new index if chart with specified
         * name and version does not exist in collection of charts which should be removed
         * from index file.
         * @param lines Parsed lines
         * @param pckgs Charts which should be removed
         * @param writer Writer
         * @throws IOException In case of exception during writing
         */
        private static void writeIfNotContainInDeleted(
            final List<String> lines,
            final Map<String, Set<String>> pckgs,
            final YamlWriter writer
        ) throws IOException {
            final ChartVersions items = new ChartVersions(lines);
            final String name = items.name().trim().replace(":", "");
            final Map<String, List<String>> vrsns = items.versions();
            boolean recordedname = false;
            if (pckgs.containsKey(name)) {
                for (final String vers : vrsns.keySet()) {
                    if (!pckgs.get(name).contains(vers)) {
                        if (!recordedname) {
                            recordedname = true;
                            writer.writeLine(items.name(), 0);
                        }
                        final List<String> entry = vrsns.get(vers);
                        for (final String line : entry) {
                            writer.writeLine(line, 0);
                        }
                    }
                }
            } else {
                for (final String line : lines) {
                    writer.writeLine(line, 0);
                }
            }
            lines.clear();
        }

        /**
         * Checks whether all charts with specified versions exist in index file,
         * in case of absence one of them an exception will be thrown.
         * @param fromidx Charts with specified versions from index file
         * @param todelete Charts with specified versions which should be deleted
         */
        private static void checkExistenceChartsToDelete(
            final Map<String, Set<String>> fromidx,
            final Map<String, Set<String>> todelete
        ) {
            for (final String pckg : todelete.keySet()) {
                if (!fromidx.containsKey(pckg)) {
                    throw new ArtipieException(
                        new IllegalStateException(
                            String.format(
                                "Failed to delete package `%s` as it is absent in index", pckg
                            )
                        )
                    );
                }
                for (final String vrsn : todelete.get(pckg)) {
                    if (!fromidx.get(pckg).contains(vrsn)) {
                        // @checkstyle LineLengthCheck (5 lines)
                        throw new ArtipieException(
                            new IllegalStateException(
                                String.format(
                                    "Failed to delete package `%s` with version `%s` as it is absent in index",
                                    pckg,
                                    vrsn
                                )
                            )
                        );
                    }
                }
            }
        }

        /**
         * Extracts versions for chart from passed parsed lines.
         * @since 0.3
         */
        private static final class ChartVersions {
            /**
             * Parsed lines.
             */
            private final List<String> lines;

            /**
             * First line should contain name of chart. It is important that
             * these lines are not trimmed.
             * @param lines Parsed lines from index file
             */
            ChartVersions(final List<String> lines) {
                this.lines = lines;
            }

            /**
             * Extracts versions from parsed lines. It is necessary because one chart can
             * have many versions and parsed lines contain all of them.
             * @return Map with info from index file for each version of chart.
             */
            public Map<String, List<String>> versions() {
                final Map<String, List<String>> vrsns = new HashMap<>();
                final char dash = '-';
                if (this.lines.size() > 1) {
                    final int indent = this.lines.get(1).indexOf(dash);
                    final List<String> tmp = new ArrayList<>(2);
                    for (int idx = 1; idx < this.lines.size(); idx += 1) {
                        if (this.lines.get(idx).charAt(indent) == dash && !tmp.isEmpty()) {
                            vrsns.put(version(tmp), new ArrayList<>(tmp));
                            tmp.clear();
                        }
                        tmp.add(this.lines.get(idx));
                    }
                    vrsns.put(version(tmp), new ArrayList<>(tmp));
                }
                return vrsns;
            }

            /**
             * Obtains name of chart.
             * @return Name of chart.
             */
            public String name() {
                if (!this.lines.isEmpty()) {
                    return this.lines.get(0);
                }
                throw new IllegalStateException("Failed to get name as there are no lines");
            }

            /**
             * Extracts version from parsed lines.
             * @param entry Parsed lines from index with version
             * @return Version from parsed lines.
             */
            private static String version(final List<String> entry) {
                return entry.stream().filter(
                    line -> line.trim().startsWith(Asto.VRSNS)
                ).map(line -> line.replace(Asto.VRSNS, ""))
                    .map(String::trim)
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalStateException("Couldn't find version for deletion")
                    );
            }
        }

        /**
         * Class for saving context during processing of index file.
         * It is not thread safe but {@code scan()} operation serially processes file line by line.
         * @since 1.1.0
         */
        private static final class ScanContext {
            /**
             * Is it an entries section?
             */
            private boolean inentries;

            /**
             * Yaml writer.
             */
            private YamlWriter wrtr;

            /**
             * Latest valid parsed name of chart from index file.
             */
            private String name;

            /**
             * Part of lines from index file.
             */
            private final List<String> lines;

            /**
             * Ctor with default yaml writer.
             * @param bufw Writer
             * @param indent Required indent in index file
             */
            ScanContext(final BufferedWriter bufw, final int indent) {
                this.wrtr = new YamlWriter(bufw, indent);
                this.name = "";
                this.lines = new ArrayList<>(2);
            }

            /**
             * Update value of location of latest written line.
             * @param inentrs Is it an entries section?
             */
            private void setEntries(final boolean inentrs) {
                this.inentries = inentrs;
            }

            /**
             * Update value of name.
             * @param cname New name
             */
            private void setName(final String cname) {
                this.name = cname;
            }

            /**
             * Update value of writer.
             * @param writer New yaml writer
             */
            private void setWriter(final YamlWriter writer) {
                this.wrtr = writer;
            }

            /**
             * Adds line from index file.
             * @param line Line from index file
             */
            private void addLine(final String line) {
                this.lines.add(line);
            }
        }
    }
}
