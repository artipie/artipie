/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Record consumer.
 * @since 0.31
 * @checkstyle MagicNumberCheck (1000 lines)
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods", "PMD.CheckResultSet", "PMD.CloseResource"}
)
class DbConsumerTest {

    /**
     * Test directory.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @TempDir
    Path path;

    /**
     * Test connection.
     */
    private Connection connection;

    @BeforeEach
    void init() throws SQLException {
        this.connection = new ArtifactDbFactory(Yaml.createYamlMappingBuilder().build(), this.path)
            .initialize().getConnection();
    }

    @Test
    void addsAndRemovesRecord() throws SQLException {
        final long created = System.currentTimeMillis();
        final ArtifactEvent record = new ArtifactEvent(
            "rpm", "my-rpm", "Alice", "org.time", "1.2", 1250L, created
        );
        new DbConsumer(this.connection).accept(record);
        try (Statement stat = this.connection.createStatement()) {
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
        new DbConsumer(this.connection).accept(
            new ArtifactEvent(
                "rpm", "my-rpm", "Alice", "org.time", "1.2", 1250L, created,
                ArtifactEvent.Type.DELETE
            )
        );
        try (Statement stat = this.connection.createStatement()) {
            stat.execute("select count(*) from artifacts");
            MatcherAssert.assertThat(
                stat.getResultSet().getInt(1),
                new IsEqual<>(0)
            );
        }
    }

    @AfterEach
    void close() throws SQLException {
        this.connection.close();
    }
}
