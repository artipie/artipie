/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
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
 * Benchmark for {@link CondaRepodata.Append}.
 * @since 0.2
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class CondaRepodataAppendBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark repodata.json.
     */
    private byte[] repodata;

    /**
     * New packages to append.
     */
    private List<TestPackage> pckg;

    @Setup
    public void setup() throws IOException {
        if (CondaRepodataAppendBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        this.pckg = new ArrayList<>();
        try (Stream<Path> stream = Files.list(Paths.get(CondaRepodataAppendBench.BENCH_DIR))) {
            for (final Path file : stream.collect(Collectors.toList())) {
                final byte[] bytes = Files.readAllBytes(file);
                final String name = file.getFileName().toString();
                if (name.endsWith("repodata.json")) {
                    this.repodata = bytes;
                } else if (name.endsWith(".tar.bz2") || name.endsWith(".conda")) {
                    this.pckg.add(
                        new TestPackage(
                            bytes, name, DigestUtils.sha256Hex(bytes), DigestUtils.md5Hex(bytes)
                        )
                    );
                }
            }
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) {
        new CondaRepodata.Append(
            new ByteArrayInputStream(this.repodata), new ByteArrayOutputStream()
        ).perform(
            this.pckg.stream().map(
                item -> new CondaRepodata.PackageItem(
                    new ByteArrayInputStream(item.input), item.filename, item.sha256, item.md5,
                    item.input.length
                )
            ).collect(Collectors.toList())
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
                .include(CondaRepodataAppendBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }

    /**
     * Package item: .conda or tar.bz2 package as bytes, file name and checksums.
     * @since 0.2
     * @checkstyle ParameterNameCheck (100 lines)
     */
    private static final class TestPackage {

        /**
         * Package bytes.
         */
        private final byte[] input;

        /**
         * Name of the file.
         */
        private final String filename;

        /**
         * Sha256 sum of the package.
         * @checkstyle MemberNameCheck (5 lines)
         */
        private final String sha256;

        /**
         * Md5 sum of the package.
         * @checkstyle MemberNameCheck (5 lines)
         */
        private final String md5;

        /**
         * Ctor.
         * @param input Package input stream
         * @param filename Name of the file
         * @param sha256 Sha256 sum of the package
         * @param md5 Md5 sum of the package
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        public TestPackage(final byte[] input, final String filename, final String sha256,
            final String md5) {
            this.input = input;
            this.filename = filename;
            this.sha256 = sha256;
            this.md5 = md5;
        }
    }
}
