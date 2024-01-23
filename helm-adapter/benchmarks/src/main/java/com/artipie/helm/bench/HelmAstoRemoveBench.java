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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
 * Benchmark for {@link com.artipie.helm.Helm.Asto#delete}.
 * @since 0.3
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class HelmAstoRemoveBench {
    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Collection of keys of files which should be removed.
     */
    private Set<Key> todelete;

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
        if (HelmAstoRemoveBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        this.todelete = Stream.of(
            "moodle-7.2.8.tgz",
            "msoms-0.2.2.tgz",
            "mssql-linux-0.11.4.tgz",
            "rethinkdb-1.1.4.tgz",
            "spring-cloud-data-flow-2.8.1.tgz",
            "quassel-0.2.13.tgz",
            "rocketchat-2.0.10.tgz",
            "oauth2-proxy-1.0.0.tgz", "oauth2-proxy-1.0.1.tgz", "oauth2-proxy-1.1.0.tgz",
            "prometheus-11.11.0.tgz", "prometheus-11.11.1.tgz",
            "parse-6.2.16.tgz", "parse-7.0.0.tgz", "parse-7.1.0.tgz"
        ).map(Key.From::new)
        .collect(Collectors.toSet());
        this.inmemory = new InMemoryStorage();
        try (Stream<Path> files = Files.list(Paths.get(HelmAstoRemoveBench.BENCH_DIR))) {
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
            .delete(this.todelete, Key.ROOT)
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
                .include(HelmAstoRemoveBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }
}
