/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.artipie.scheduling.ArtifactEvent;
import com.jcabi.log.Logger;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.sql.DataSource;

/**
 * Consumer for artifact records which writes the records into db.
 * @since 0.31
 */
public final class DbConsumer implements Consumer<ArtifactEvent> {

    /**
     * Publish subject
     * <a href="https://reactivex.io/documentation/subject.html">Docs</a>.
     */
    private final PublishSubject<ArtifactEvent> subject;

    /**
     * Database source.
     */
    private final DataSource source;

    /**
     * Ctor.
     * @param source Database source
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public DbConsumer(final DataSource source) {
        this.source = source;
        this.subject = PublishSubject.create();
        // @checkstyle MagicNumberCheck (5 lines)
        this.subject.subscribeOn(Schedulers.io())
            .buffer(2, TimeUnit.SECONDS, 50)
            .subscribe(new DbObserver());
    }

    @Override
    public void accept(final ArtifactEvent record) {
        this.subject.onNext(record);
    }

    /**
     * Database observer. Writes pack into database.
     * @since 0.31
     */
    private final class DbObserver implements Observer<List<ArtifactEvent>> {

        @Override
        public void onSubscribe(final @NonNull Disposable disposable) {
            Logger.debug(this, "Subscribed to insert/delete db records");
        }

        // @checkstyle ExecutableStatementCountCheck (40 lines)
        @Override
        public void onNext(final @NonNull List<ArtifactEvent> events) {
            if (events.isEmpty()) {
                return;
            }
            final List<ArtifactEvent> errors = new ArrayList<>(events.size());
            boolean error = false;
            try (
                Connection conn = DbConsumer.this.source.getConnection();
                PreparedStatement insert = conn.prepareStatement(
                    // @checkstyle LineLengthCheck (1 line)
                    "insert or replace into artifacts (repo_type, repo_name, name, version, size, created_date, owner) VALUES (?,?,?,?,?,?,?);"
                );
                PreparedStatement deletev = conn.prepareStatement(
                    "delete from artifacts where repo_name = ? and name = ? and version = ?;"
                );
                PreparedStatement delete = conn.prepareStatement(
                    "delete from artifacts where repo_name = ? and name = ?;"
                )
            ) {
                conn.setAutoCommit(false);
                for (final ArtifactEvent record : events) {
                    try {
                        if (record.eventType() == ArtifactEvent.Type.INSERT) {
                            //@checkstyle MagicNumberCheck (20 lines)
                            insert.setString(1, record.repoType());
                            insert.setString(2, record.repoName());
                            insert.setString(3, record.artifactName());
                            insert.setString(4, record.artifactVersion());
                            insert.setDouble(5, record.size());
                            insert.setDate(6, new Date(record.createdDate()));
                            insert.setString(7, record.owner());
                            insert.execute();
                        } else if (record.eventType() == ArtifactEvent.Type.DELETE_VERSION) {
                            deletev.setString(1, record.repoName());
                            deletev.setString(2, record.artifactName());
                            deletev.setString(3, record.artifactVersion());
                            deletev.execute();
                        } else if (record.eventType() == ArtifactEvent.Type.DELETE_ALL) {
                            delete.setString(1, record.repoName());
                            delete.setString(2, record.artifactName());
                            delete.execute();
                        }
                    } catch (final SQLException ex) {
                        Logger.error(this, ex.getMessage());
                        errors.add(record);
                    }
                }
                conn.commit();
            } catch (final SQLException ex) {
                Logger.error(this, ex.getMessage());
                events.forEach(DbConsumer.this.subject::onNext);
                error = true;
            }
            if (!error) {
                errors.forEach(DbConsumer.this.subject::onNext);
            }
        }

        @Override
        public void onError(final @NonNull Throwable error) {
            Logger.error(this, "Fatal error!");
        }

        @Override
        public void onComplete() {
            Logger.debug(this, "Subscription cancelled");
        }
    }
}
