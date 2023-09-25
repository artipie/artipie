/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.testcontainers.containers.BindMode;

/**
 * Integration test for artifact metadata
 * database.
 * @since 0.31
 */
public class MetadataITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie-db.yaml")
            .withRepoConfig("maven/maven.yml", "my-maven")
            .withRepoConfig("maven/maven-proxy.yml", "my-maven-proxy"),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven/maven-settings.xml", "/w/settings.xml", BindMode.READ_ONLY
            )
    );

    @Test
    void deploysArtifact(@TempDir Path temp) throws Exception {
        this.containers.putBinaryToClient(
            new TestResource("helloworld-src/pom.xml").asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Deploy failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", "settings.xml", "deploy", "-Dmaven.install.skip=true"
        );
        this.containers.assertExec(
            "Download failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", "settings.xml", "-U", "dependency:get",
            "-Dartifact=com.artipie:helloworld:0.1"
        );
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                final Path data = temp.resolve(UUID.randomUUID().toString())
                    .resolve("artifacts.db");
                Files.write(data, this.containers.getArtipieContent("/var/artipie/artifacts.db"));
                final SQLiteDataSource source = new SQLiteDataSource();
                source.setUrl(String.format("jdbc:sqlite:%s", data));
                try (
                    Connection conn = source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return stat.getResultSet().getInt(1) == 1;
                }
            }
        );
    }

}
