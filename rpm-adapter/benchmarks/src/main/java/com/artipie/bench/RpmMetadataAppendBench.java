/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.bench;

import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RpmMetadata;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.artipie.rpm.pkg.Package;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for {@link RpmMetadata.Append}.
 * @since 1.4
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class RpmMetadataAppendBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark metadata.
     */
    private Map<XmlPackage, byte[]> items;

    /**
     * Benchmark rpms.
     */
    private Collection<Package.Meta> rpms;

    @Setup
    public void setup() throws IOException {
        if (RpmMetadataAppendBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> files = Files.list(Paths.get(RpmMetadataAppendBench.BENCH_DIR))) {
            final List<Path> flist = files.collect(Collectors.toList());
            this.items = flist.stream().map(
                file -> new XmlPackage.Stream(true).get()
                    .filter(xml -> file.toString().contains(xml.lowercase()))
                    .findFirst().map(
                        item -> new ImmutablePair<>(
                            item,
                            new UncheckedIOScalar<>(() -> Files.readAllBytes(file)).value()
                        )
                    )
            ).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
            this.rpms = flist.stream().filter(item -> item.endsWith(".rpm"))
                .map(
                    item -> new FilePackage.Headers(
                        new UncheckedIOScalar<>(() -> new FilePackageHeader(item).header()).value(),
                        item, Digest.SHA256, item.getFileName().toString()
                    )
                ).collect(Collectors.toList());
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) throws IOException {
        new RpmMetadata.Append(
            this.items.entrySet().stream()
            .map(
                entry -> new RpmMetadata.MetadataItem(
                    entry.getKey(),
                    new ByteArrayInputStream(entry.getValue()),
                    new ByteArrayOutputStream()
                )
            ).toArray(RpmMetadata.MetadataItem[]::new)
        ).perform(this.rpms);
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(RpmMetadataAppendBench.class.getSimpleName()).forks(1).build()
        ).run();
    }

}
