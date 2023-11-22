/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.bench;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.memory.BenchmarkStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.helm.Helm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for {@link com.artipie.helm.Helm.Asto#reindex(Key)}.
 * @since 0.3
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle DesignForExtensionCheck (500 lines)
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class HelmAstoReindexBench {
    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Backend in memory storage. Should be used only for reading
     * after saving required information on preparation stage.
     */
    private InMemoryStorage inmemory;

    /**
     * Implementation of storage for benchmarks.
     */
    private BenchmarkStorage benchstrg;

    @Setup
    public void setup() throws IOException {
        if (HelmAstoReindexBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        this.inmemory = new InMemoryStorage();
        try (Stream<Path> files = Files.list(Paths.get(HelmAstoReindexBench.BENCH_DIR))) {
            files.forEach(
                file -> this.inmemory.save(
                    new Key.From(file.getFileName().toString()),
                    new Content.From(
                        new UncheckedIOScalar<>(() -> Files.readAllBytes(file)).value()
                    )
                ).join()
            );
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        this.benchstrg = new BenchmarkStorage(this.inmemory);
    }

    @Benchmark
    public void run() {
        new Helm.Asto(this.benchstrg)
            .reindex(Key.ROOT)
            .toCompletableFuture().join();
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(HelmAstoReindexBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }
}
