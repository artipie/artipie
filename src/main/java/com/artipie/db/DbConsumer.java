/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.EventProcessingError;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Consumer for artifact records which writes the records into db.
 * @since 0.31
 */
public final class DbConsumer implements Consumer<ArtifactEvent> {

    /**
     * Database source.
     */
    private final Connection conn;

    /**
     * Ctor.
     * @param conn Database source
     */
    public DbConsumer(final Connection conn) {
        this.conn = conn;
    }

    @Override
    public void accept(final ArtifactEvent record) {
        if (record.eventType() == ArtifactEvent.Type.INSERT) {
            try (
                //@checkstyle LineLengthCheck (2 lines)
                PreparedStatement statement = this.conn.prepareStatement(
                    "insert into artifacts (repo_type, repo_name, name, version, size, created_date, owner) VALUES (?,?,?,?,?,?,?);"
                )
            ) {
                //@checkstyle MagicNumberCheck (10 lines)
                statement.setString(1, record.repoType());
                statement.setString(2, record.repoName());
                statement.setString(3, record.artifactName());
                statement.setString(4, record.artifactVersion());
                statement.setDouble(5, record.size());
                statement.setDate(6, new Date(record.createdDate()));
                statement.setString(7, record.owner());
                statement.execute();
            } catch (final SQLException error) {
                throw new EventProcessingError("Error while inserting record", error);
            }
        } else if (record.eventType() == ArtifactEvent.Type.DELETE) {
            try (
                PreparedStatement statement = this.conn.prepareStatement(
                    "delete from artifacts where repo_name = ? and name = ? and version = ?;"
                )
            ) {
                //@checkstyle MagicNumberCheck (10 lines)
                statement.setString(1, record.repoName());
                statement.setString(2, record.artifactName());
                statement.setString(3, record.artifactVersion());
                statement.execute();
            } catch (final SQLException error) {
                throw new EventProcessingError("Error while removing record", error);
            }
        }
    }
}
