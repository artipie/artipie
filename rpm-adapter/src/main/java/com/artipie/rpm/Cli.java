/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import java.nio.file.Path;

/**
 * Cli tool main class.
 *
 * @since 0.6
 */
public final class Cli {

    /**
     * Rpm tool.
     */
    private final Rpm rpm;

    /**
     * Ctor.
     * @param rpm Rpm instance
     */
    private Cli(final Rpm rpm) {
        this.rpm = rpm;
    }

    /**
     * Main method of Cli tool.
     *
     * @param args Arguments of command line
             */
    @SuppressWarnings(
        {
            "PMD.SystemPrintln",
            "PMD.AvoidCatchingGenericException",
            "PMD.AvoidDuplicateLiterals"
        }
    )
    public static void main(final String... args) {
        final CliArguments cliargs = new CliArguments(args);
        final RepoConfig cnfg = cliargs.config();
        final NamingPolicy naming = cnfg.naming();
        System.out.printf("RPM naming-policy=%s\n", naming);
        final Digest digest = cnfg.digest();
        System.out.printf("RPM digest=%s\n", digest);
        final boolean filelists = cnfg.filelists();
        System.out.printf("RPM file-lists=%s\n", filelists);
        final Path repository = cliargs.repository();
        System.out.printf("RPM repository=%s\n", repository);
        try {
            new Cli(
                new Rpm(
                    new FileStorage(repository),
                    naming,
                    digest,
                    filelists
                )
            ).run();
        } catch (final Exception err) {
            System.err.printf("RPM failed: %s\n", err.getLocalizedMessage());
            err.printStackTrace(System.err);
        }
    }

    /**
     * Run CLI tool.
     */
    private void run() {
        this.rpm.batchUpdate(Key.ROOT).blockingAwait();
    }
}
