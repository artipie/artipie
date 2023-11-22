/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.bench;

import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.rpm.RpmMetadata;
import com.artipie.rpm.meta.XmlPackage;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Benchmark for {@link com.artipie.rpm.RpmMetadata.Remove}.
 * @since 1.4
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
public class RpmMetadataRemoveBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark metadata.
     */
    private Map<XmlPackage, byte[]> items;

    @Setup
    public void setup() throws IOException {
        if (RpmMetadataRemoveBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> files = Files.list(Paths.get(RpmMetadataRemoveBench.BENCH_DIR))) {
            this.items = files.map(
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
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) throws IOException {
        new RpmMetadata.Remove(
            this.items.entrySet().stream()
            .map(
                entry -> new RpmMetadata.MetadataItem(
                    entry.getKey(),
                    new ByteArrayInputStream(entry.getValue()),
                    new ByteArrayOutputStream()
                )
            ).toArray(RpmMetadata.MetadataItem[]::new)
        ).perform(
            Lists.newArrayList(
                "35f6b7ceecb3b66d41991358113ae019dbabbac21509afbe770c06d6999d75c7",
                "8dad6a68a8868c7e4595634affbad8677e48e259dac9180dd73a41ae8414305a",
                "0ab1a22f716b480392a3fe28e9fafebd61ff8afe3196aa35ccc937413e0a3c4a",
                "8440d6772087e9b4f0c3db57eb328594d1c18cdacd52f3565cba87fb0ce0cc0d",
                "3f7e099180803c182194a5277fe6d7e2561550ca51598d5bc3334c11361090af",
                "2cbe8499cd1c48e0440bcf0a8e4a1e4a336142d521db91e35a546ec99f7c50ac",
                "05c37cb7b04bfe885f139340fb58aa8e0051b62e4215feded619a8cc726609a3",
                "3d81ad4030684e997772d2bdf1dd5d8253fb66df68e30a09507aafb49ae359f6",
                "5eb3cc0a41ea8770c2c4491e7d574e263aa3ae3bb1006a4b9b883abbd58cbfd9",
                "2b068b878a023ebd9bec65767dea211035dbb2d72470fba08b8ca36a130cc5ec",
                "5104d2c1feecedc2f19778219530f2f7731b296c5957659aaddc5968a555a020"
            )
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
                .include(RpmMetadataRemoveBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }

}
