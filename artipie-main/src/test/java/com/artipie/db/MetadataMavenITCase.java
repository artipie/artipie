/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.db;

import com.artipie.asto.misc.UncheckedSupplier;
import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

/**
 * Integration test for artifact metadata
 * database.
 * @since 0.31
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MetadataMavenITCase {

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie-db.yaml")
            .withRepoConfig("maven/maven.yml", "my-maven")
            .withRepoConfig("maven/maven-proxy.yml", "my-maven-proxy"),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
    );

    @Test
    void deploysArtifactIntoMaven(final @TempDir Path temp) throws Exception {
        this.containers.putClasspathResourceToClient("maven/maven-settings.xml", "/w/settings.xml");
        this.containers.putBinaryToClient(
            new TestResource("helloworld-src/pom.xml").asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Deploy failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", "settings.xml", "deploy", "-Dmaven.install.skip=true"
        );
        this.containers.putBinaryToClient(
            new TestResource("snapshot-src/pom.xml").asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Deploy failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", "settings.xml", "deploy", "-Dmaven.install.skip=true"
        );
        awaitDbRecords(
            this.containers, temp, rs -> new UncheckedSupplier<>(() -> rs.getInt(1) == 2).get()
        );
    }

    @Test
    void downloadFromProxy(final @TempDir Path temp) throws IOException {
        this.containers.putClasspathResourceToClient(
            "maven/maven-settings-proxy-metadata.xml", "/w/settings.xml"
        );
        this.containers.putBinaryToClient(
            new TestResource("maven/pom-with-deps/pom.xml").asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Uploading dependencies failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-s", "settings.xml", "dependency:resolve"
        );
        awaitDbRecords(
            this.containers, temp, rs -> new UncheckedSupplier<>(() -> rs.getInt(1) > 300).get()
        );
    }

    static void awaitDbRecords(
        final TestDeployment containers, final Path temp, final Predicate<ResultSet> condition
    ) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                final Path data = temp.resolve(String.format("%s-artifacts.db", UUID.randomUUID()));
                Files.write(data, containers.getArtipieContent("/var/artipie/artifacts.db"));
                final SQLiteDataSource source = new SQLiteDataSource();
                source.setUrl(String.format("jdbc:sqlite:%s", data));
                try (
                    Connection conn = source.getConnection();
                    Statement stat = conn.createStatement()
                ) {
                    stat.execute("select count(*) from artifacts");
                    return condition.test(stat.getResultSet());
                }
            }
        );
    }

}
