/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

/**
 * Factory to create and initialize artifacts SqLite database.
 * <p/>
 * Factory accepts Artipie yaml settings file and creates database source and database structure.
 * Is settings are absent in config yaml, db file is created in the provided `def` directory.
 * <p/>
 * Artifacts db settings section in artipie yaml:
 * <pre>{@code
 * artifacts_database:
 *   sqlite_data_file_path: test.db # required, the path to the SQLite database file,
 *       which is either relative or absolute
 *   threads_count: 3 # default 1, not required, in how many parallel threads to
 *       process artifacts data queue
 *   interval_seconds: 5 # default 1, not required, interval to check events queue and write into db
 * }</pre>
 * @since 0.31
 */
public final class ArtifactDbFactory {

    /**
     * Sqlite database file path.
     */
    static final String YAML_PATH = "sqlite_data_file_path";

    /**
     * Sqlite database default file name.
     */
    static final String DB_NAME = "artifacts.db";

    /**
     * Settings yaml.
     */
    private final YamlMapping yaml;

    /**
     * Default path to create database file.
     */
    private final Path def;

    /**
     * Ctor.
     * @param yaml Settings yaml
     * @param def Default location for db file
     */
    public ArtifactDbFactory(final YamlMapping yaml, final Path def) {
        this.yaml = yaml;
        this.def = def;
    }

    /**
     * Initialize artifacts database and mechanism to gather artifacts metadata and
     * write to db.
     * If yaml settings are absent, default path and db name are used.
     * @return Queue to add artifacts metadata into
     * @throws ArtipieException On error
     */
    public DataSource initialize() {
        final YamlMapping config = this.yaml.yamlMapping("artifacts_database");
        final String path;
        if (config == null || config.string(ArtifactDbFactory.YAML_PATH) == null) {
            path = this.def.resolve(ArtifactDbFactory.DB_NAME).toAbsolutePath().toString();
        } else {
            path = config.string(ArtifactDbFactory.YAML_PATH);
        }
        final SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl(String.format("jdbc:sqlite:%s", path));
        ArtifactDbFactory.createStructure(source);
        return source;
    }

    /**
     * Create db structure to write artifacts data.
     * @param source Database source
     * @throws ArtipieException On error
     */
    private static void createStructure(final DataSource source) {
        try (Connection conn = source.getConnection();
            Statement statement = conn.createStatement()) {
            statement.executeUpdate(
                String.join(
                    "\n",
                    "create TABLE if NOT EXISTS artifacts(",
                    "   id BIGINT PRIMARY KEY AUTOINCREMENT,",
                    "   repo_type CHAR(10) NOT NULL,",
                    "   repo_name CHAR(20) NOT NULL,",
                    "   name VARCHAR NOT NULL,",
                    "   version VARCHAR NOT NULL,",
                    "   size BIGINT NOT NULL,",
                    "   created_date DATETIME NOT NULL,",
                    "   owner VARCHAR NOT NULL,",
                    "   UNIQUE (repo_name, name, version) ",
                    ");"
                )
            );
        } catch (final SQLException error) {
            throw new ArtipieException(error);
        }
    }
}
