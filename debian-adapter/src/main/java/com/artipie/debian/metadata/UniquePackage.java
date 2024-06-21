/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.StorageValuePipeline;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implementation of {@link Package} that checks uniqueness of the packages index records.
 * @since 0.5
 */
public final class UniquePackage implements Package {

    /**
     * Package index items separator.
     */
    private static final String SEP = "\n\n";

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public UniquePackage(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletionStage<Void> add(final Iterable<String> items, final Key index) {
        return new StorageValuePipeline<List<String>>(this.asto, index).processWithResult(
            (opt, out) -> {
                List<String> duplicates = Collections.emptyList();
                if (opt.isPresent()) {
                    duplicates = UniquePackage.decompressAppendCompress(opt.get(), out, items);
                } else {
                    UniquePackage.compress(items, out);
                }
                return duplicates;
            }
        ).thenCompose(this::remove);
    }

    public CompletionStage<Void> delete(final Iterable<String> items, final Key index) {
        return new StorageValuePipeline<List<String>>(this.asto, index, new Key.From(index.string() + "_new")).processWithResult(
                (opt, out) -> {
                    List<String> duplicates = Collections.emptyList();
                    if (opt.isPresent()) {
                        duplicates = UniquePackage.decompressRemoveCompress(opt.get(), out, items);
                    } else {
                        UniquePackage.compress(items, out);
                    }
                    return duplicates;
                }
        ).thenCompose(
                nothing -> this.asto.delete(index).thenCompose(
                        nothing1 -> this.asto.move(new Key.From(index.string() + "_new"), index)
                )
        );
    }

    /**
     * Removes storage item from provided keys.
     * @param keys Keys list
     * @return Completed action
     */
    private CompletionStage<Void> remove(final List<String> keys) {
        return CompletableFuture.allOf(
            keys.stream().map(Key.From::new)
            .map(
                key -> this.asto.exists(key).thenCompose(
                    exists -> {
                        final CompletionStage<Void> res;
                        if (exists) {
                            res = this.asto.delete(key);
                        } else {
                            res = CompletableFuture.allOf();
                        }
                        return res;
                    }
                )
            ).toArray(CompletableFuture[]::new)
        );
    }

    /**
     * Decompresses Packages.gz file, checks the duplicates, appends information and writes
     * compressed result into new file.
     * @param decompress File to decompress
     * @param res Where to write the result
     * @param items Items to append
     * @return List of the `Filename`s fields of the duplicated packages.
     */
    @SuppressWarnings({"PMD.AssignmentInOperand", "PMD.CyclomaticComplexity"})
    private static List<String> decompressAppendCompress(
        final InputStream decompress, final OutputStream res, final Iterable<String> items
    ) {
        final byte[] bytes = String.join(UniquePackage.SEP, items).getBytes(StandardCharsets.UTF_8);
        final Set<Pair<String, String>> newbies = StreamSupport.stream(items.spliterator(), false)
            .<Pair<String, String>>map(
                item -> new ImmutablePair<>(
                    new ControlField.Package().value(item).get(0),
                    new ControlField.Version().value(item).get(0)
                )
            ).collect(Collectors.toSet());
        final List<String> duplicates = new ArrayList<>(5);
        try (
            GZIPInputStream gis = new GZIPInputStream(decompress);
            BufferedReader rdr =
                new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
            GZIPOutputStream gop = new GZIPOutputStream(new BufferedOutputStream(res))
        ) {
            String line;
            StringBuilder item = new StringBuilder();
            do {
                line = rdr.readLine();
                if ((line == null || line.isEmpty()) && item.length() > 0) {
                    final Optional<String> dupl = UniquePackage.duplicate(item.toString(), newbies);
                    if (dupl.isPresent()) {
                        duplicates.add(dupl.get());
                    } else {
                        gop.write(item.append('\n').toString().getBytes(StandardCharsets.UTF_8));
                    }
                    item = new StringBuilder();
                } else if (line != null && !line.isEmpty()) {
                    item.append(line).append('\n');
                }
            } while (line != null);
            gop.write(bytes);
        } catch (final UnsupportedEncodingException err) {
            throw new IllegalStateException(err);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return duplicates;
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "PMD.CyclomaticComplexity"})
    private static List<String> decompressRemoveCompress(
            final InputStream decompress, final OutputStream res, final Iterable<String> items
    ) {
        final Set<Pair<String, String>> toDelete = StreamSupport.stream(items.spliterator(), false)
                .<Pair<String, String>>map(
                        item -> new ImmutablePair<>(
                                new ControlField.Package().value(item).get(0),
                                new ControlField.Version().value(item).get(0)
                        )
                ).collect(Collectors.toSet());
        final List<String> deleted = new ArrayList<>(5);
        try (
                GZIPInputStream gis = new GZIPInputStream(decompress);
                BufferedReader rdr =
                        new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
                GZIPOutputStream gop = new GZIPOutputStream(new BufferedOutputStream(res))
        ) {
            String line;
            StringBuilder item = new StringBuilder();
            do {
                line = rdr.readLine();
                if ((line == null || line.isEmpty()) && item.length() > 0) {
                    final Optional<String> dupl = UniquePackage.duplicate(item.toString(), toDelete);
                    if (dupl.isEmpty()) {
                        gop.write(item.append('\n').toString().getBytes(StandardCharsets.UTF_8));
                    }
                    item = new StringBuilder();
                } else if (line != null && !line.isEmpty()) {
                    item.append(line).append('\n');
                }
            } while (line != null);
        } catch (final UnsupportedEncodingException err) {
            throw new IllegalStateException(err);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return deleted;
    }

    /**
     * Checks whether item is present in the list of new packages to add. If so, returns package
     * `Filename` field.
     * @param item Packages item to check
     * @param newbies Newly added packages names and versions
     * @return Filename field value if package is a duplicate
     */
    private static Optional<String> duplicate(
        final String item, final Set<Pair<String, String>> newbies
    ) {
        final Pair<String, String> pair = new ImmutablePair<>(
            new ControlField.Package().value(item).get(0),
            new ControlField.Version().value(item).get(0)
        );
        Optional<String> res = Optional.empty();
        if (newbies.contains(pair)) {
            res = Optional.of(new ControlField.Filename().value(item).get(0));
        }
        return res;
    }

    /**
     * Compress text for new Package index.
     * @param items Items to compress
     * @param res Output stream to write the result
     */
    private static void compress(final Iterable<String> items, final OutputStream res) {
        try (GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(res)) {
            gcos.write(String.join(UniquePackage.SEP, items).getBytes(StandardCharsets.UTF_8));
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
