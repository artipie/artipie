/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.db.ArtifactDbFactory;
import com.artipie.db.DbConsumer;
import com.artipie.db.PostgreSQLTestConfig;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.QuartzService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test for {@link QuartzService} and
 * {@link com.artipie.db.DbConsumer}.
 * @since 0.31
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@Testcontainers
public final class SchedulerDbTest {

    /**
     * PostgreSQL test container.
     */
    @Container
    static final PostgreSQLContainer<?> POSTGRES = PostgreSQLTestConfig.createContainer();

    /**
     * Test connection.
     */
    private DataSource source;

    /**
     * Quartz service to test.
     */
    private QuartzService service;

    @BeforeEach
    void init() {
        this.source = new ArtifactDbFactory(
            Yaml.createYamlMappingBuilder().add(
                "artifacts_database",
                Yaml.createYamlMappingBuilder()
                    .add(ArtifactDbFactory.YAML_HOST, POSTGRES.getHost())
                    .add(ArtifactDbFactory.YAML_PORT, String.valueOf(POSTGRES.getFirstMappedPort()))
                    .add(ArtifactDbFactory.YAML_DATABASE, POSTGRES.getDatabaseName())
                    .add(ArtifactDbFactory.YAML_USER, POSTGRES.getUsername())
                    .add(ArtifactDbFactory.YAML_PASSWORD, POSTGRES.getPassword())
                    .build()
            ).build(),
            "artifacts"
        ).initialize();
        this.service = new QuartzService();
    }

    @AfterEach
    void stop() {
        this.service.stop();
    }

    @Test
    void insertsRecords() throws SchedulerException, InterruptedException {
        this.service.start();
        final Queue<ArtifactEvent> queue = this.service.addPeriodicEventsProcessor(
            1, List.of(new DbConsumer(this.source), new DbConsumer(this.source))
        );
        Thread.sleep(500);
        final long created = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            queue.add(
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
                    stat.execute("SELECT COUNT(*) FROM artifacts");
                    final ResultSet rs = stat.getResultSet();
                    rs.next();
                    return rs.getInt(1) == 1000;
                }
            }
        );
    }

}
