/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.debian.benchmarks;

import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.debian.MultiPackages;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * Benchmark for {@link com.artipie.debian.MultiPackages.Unique}.
 * @since 0.8
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class IndexMergeBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Input data.
     */
    private List<byte[]> input;

    @Setup
    public void setup() throws IOException {
        if (IndexMergeBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> files = Files.list(Paths.get(IndexMergeBench.BENCH_DIR))) {
            this.input = files.map(
                path -> new UncheckedIOScalar<>(() -> Files.readAllBytes(path)).value()
            ).collect(Collectors.toList());
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) throws IOException {
        new MultiPackages.Unique().merge(
            this.input.stream().map(ByteArrayInputStream::new).collect(Collectors.toList()),
            new ByteArrayOutputStream()
        );
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(IndexMergeBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }
}
