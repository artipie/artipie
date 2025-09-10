/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.Yaml;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test for artifacts db.
 * @since 0.31
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@Testcontainers
class ArtifactDbTest {

    /**
     * PostgreSQL test container.
     */
    @Container
    static final PostgreSQLContainer<?> POSTGRES = PostgreSQLTestConfig.createContainer();

    @Test
    void createsSourceFromYamlSettings() throws SQLException {
        final DataSource source = new ArtifactDbFactory(
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
        try (
            Connection conn = source.getConnection();
            Statement stat = conn.createStatement()
        ) {
            stat.execute("SELECT COUNT(*) FROM artifacts");
            final ResultSet rs = stat.getResultSet();
            rs.next();
            MatcherAssert.assertThat(
                rs.getInt(1),
                new IsEqual<>(0)
            );
        }
    }

    @Test
    void createsSourceFromDefaultLocation() throws SQLException {
        final DataSource source = new ArtifactDbFactory(
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
        try (
            Connection conn = source.getConnection();
            Statement stat = conn.createStatement()
        ) {
            stat.execute("SELECT COUNT(*) FROM artifacts");
            final ResultSet rs = stat.getResultSet();
            rs.next();
            MatcherAssert.assertThat(
                rs.getInt(1),
                new IsEqual<>(0)
            );
        }
    }

}
