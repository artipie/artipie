/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.misc.UncheckedIOFunc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Benchmark for {@link CondaRepodata.Remove}.
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class CondaRepodataRemoveBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark data.
     */
    private byte[] bytes;

    @Setup
    public void setup() throws IOException {
        if (CondaRepodataRemoveBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> files = Files.list(Paths.get(CondaRepodataRemoveBench.BENCH_DIR))) {
            this.bytes = files.findFirst()
                .map(new UncheckedIOFunc<>(Files::readAllBytes))
                .orElseThrow(() -> new IllegalStateException("Benchmark data not found"));
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) {
        new CondaRepodata.Remove(new ByteArrayInputStream(this.bytes), new ByteArrayOutputStream())
            .perform(
                Stream.of(
                    "cfc86c5b3072e4842f41ee25b154ade9fb702ca5be553741a9df5f82c377147d",
                    "7f9fefdd763a6752734c09b1cd153c9506fda2652ade323a167ffc5eab24de5b",
                    "8f827149ec6a4d9d4e77ed03d53419a906ab2299057ea2b32d264a8f56c7aac6",
                    "fa22b4438f9f33b7a255032148760c71d6e054080ba7b5e073b88adb54935357",
                    "601dd5c4272ef0fe6453b1e209ba12c61ea6ef1cf064356a096f7eeb38fd6ffd",
                    "10e2d3f890004e01193fe55909e2ba6af29f60678fa9b457dff992a38f9340f9"
                ).collect(Collectors.toSet())
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
                .include(CondaRepodataRemoveBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }
}
