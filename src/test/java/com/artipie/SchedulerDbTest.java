/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.db.ArtifactDbFactory;
import com.artipie.db.DbConsumer;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.EventQueue;
import com.artipie.scheduling.QuartsService;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.SchedulerException;

/**
 * Test for {@link com.artipie.scheduling.QuartsService} and
 * {@link com.artipie.db.DbConsumer}.
 * @since 0.31
 * @checkstyle MagicNumberCheck (1000 lines)
 * @checkstyle IllegalTokenCheck (1000 lines)
 * @checkstyle LocalVariableNameCheck (1000 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class SchedulerDbTest {

    /**
     * Test directory.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @TempDir
    Path path;

    /**
     * Test connection.
     */
    private DataSource source;

    /**
     * Quartz service to test.
     */
    private QuartsService service;

    @BeforeEach
    void init() {
        this.source = new ArtifactDbFactory(Yaml.createYamlMappingBuilder().build(), this.path)
            .initialize();
        this.service = new QuartsService();
    }

    @Test
    void insertsRecords() throws SchedulerException, InterruptedException {
        this.service.start();
        final EventQueue<ArtifactEvent> queue = this.service.addPeriodicEventsProcessor(
            1, List.of(new DbConsumer(this.source), new DbConsumer(this.source))
        );
        Thread.sleep(500);
        final long created = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            queue.put(
                new ArtifactEvent(
                    "rpm", "my-rpm", "Alice", "org.time", String.valueOf(i), 1250L, created
                )
            );
            if (i % 50 == 0) {
                Thread.sleep(990);
            }
        }
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 1000;
                }
            }
        );
    }

}
