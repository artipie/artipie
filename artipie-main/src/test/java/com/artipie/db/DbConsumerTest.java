/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.scheduling.ArtifactEvent;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Record consumer.
 * @since 0.31
 */
@SuppressWarnings(
    {
        "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods", "PMD.CheckResultSet",
        "PMD.CloseResource", "PMD.UseUnderscoresInNumericLiterals"
    }
)
class DbConsumerTest {

    /**
     * Test directory.
     */
    @TempDir
    Path path;

    /**
     * Test connection.
     */
    private DataSource source;

    @BeforeEach
    void init() {
        this.source = new ArtifactDbFactory(Yaml.createYamlMappingBuilder().build(), this.path)
            .initialize();
    }

    @Test
    void addsAndRemovesRecord() throws SQLException, InterruptedException {
        final DbConsumer consumer = new DbConsumer(this.source);
        Thread.sleep(1000);
        final long created = System.currentTimeMillis();
        final ArtifactEvent record = new ArtifactEvent(
            "rpm", "my-rpm", "Alice", "org.time", "1.2", 1250L, created
        );
        consumer.accept(record);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 1;
                }
            }
        );
        try (
            Connection conn = this.source.getConnection();
            Statement stat = conn.createStatement()
        ) {
            stat.execute("select * from artifacts");
            final ResultSet res = stat.getResultSet();
            res.next();
            MatcherAssert.assertThat(
                res.getString("repo_type"),
                new IsEqual<>(record.repoType())
            );
            MatcherAssert.assertThat(
                res.getString("repo_name"),
                new IsEqual<>(record.repoName())
            );
            MatcherAssert.assertThat(
                res.getString("name"),
                new IsEqual<>(record.artifactName())
            );
            MatcherAssert.assertThat(
                res.getString("version"),
                new IsEqual<>(record.artifactVersion())
            );
            MatcherAssert.assertThat(
                res.getString("owner"),
                new IsEqual<>(record.owner())
            );
            MatcherAssert.assertThat(
                res.getLong("size"),
                new IsEqual<>(record.size())
            );
            MatcherAssert.assertThat(
                res.getDate("created_date"),
                new IsEqual<>(new Date(record.createdDate()))
            );
            MatcherAssert.assertThat(
                "ResultSet does not have more records",
                res.next(), new IsEqual<>(false)
            );
        }
        consumer.accept(
            new ArtifactEvent(
                "rpm", "my-rpm", "Alice", "org.time", "1.2", 1250L, created,
                ArtifactEvent.Type.DELETE_VERSION
            )
        );
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 0;
                }
            }
        );
    }

    @Test
    void insertsAndRemovesRecords() throws InterruptedException {
        final DbConsumer consumer = new DbConsumer(this.source);
        Thread.sleep(1000);
        final long created = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            consumer.accept(
                new ArtifactEvent(
                    "rpm", "my-rpm", "Alice", "org.time", String.valueOf(i), 1250L, created - i
                )
            );
            if (i % 99 == 0) {
                Thread.sleep(1000);
            }
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 500;
                }
            }
        );
        for (int i = 500; i <= 1000; i++) {
            consumer.accept(
                new ArtifactEvent(
                    "rpm", "my-rpm", "Alice", "org.time", String.valueOf(i), 1250L, created - i
                )
            );
            if (i % 99 == 0) {
                Thread.sleep(1000);
            }
            if (i % 20 == 0) {
                consumer.accept(
                    new ArtifactEvent(
                        "rpm", "my-rpm", "Alice", "org.time", String.valueOf(i - 500), 1250L,
                        created - i, ArtifactEvent.Type.DELETE_VERSION
                    )
                );
            }
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 975;
                }
            }
        );
    }

    @Test
    void removesAllByName() throws InterruptedException {
        final DbConsumer consumer = new DbConsumer(this.source);
        Thread.sleep(1000);
        final long created = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            consumer.accept(
                new ArtifactEvent(
                    "maven", "my-maven", "Alice", "com.artipie.asto",
                    String.valueOf(i), 1250L, created - i
                )
            );
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 10;
                }
            }
        );
        consumer.accept(new ArtifactEvent("maven", "my-maven", "com.artipie.asto"));
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 0;
                }
            }
        );
    }

    @Test
    void replacesOnConflict() throws InterruptedException, SQLException {
        final DbConsumer consumer = new DbConsumer(this.source);
        Thread.sleep(1000);
        final long first = System.currentTimeMillis();
        consumer.accept(
            new ArtifactEvent(
                "docker", "my-docker", "Alice", "linux/alpine", "latest", 12550L, first
            )
        );
        final long size = 56950L;
        final long second = first + 65854L;
        consumer.accept(
            new ArtifactEvent(
                "docker", "my-docker", "Alice", "linux/alpine", "latest", size, second
            )
        );
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                try (
                    Connection conn = this.source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 1;
                }
            }
        );
        try (
            Connection conn = this.source.getConnection();
            Statement stat = conn.createStatement()
        ) {
            stat.execute("select * from artifacts");
            final ResultSet res = stat.getResultSet();
            res.next();
            MatcherAssert.assertThat(
                res.getLong("size"),
                new IsEqual<>(size)
            );
            MatcherAssert.assertThat(
                res.getDate("created_date"),
                new IsEqual<>(new Date(second))
            );
            MatcherAssert.assertThat(
                "ResultSet does not have more records",
                res.next(), new IsEqual<>(false)
            );
        }
    }
}
