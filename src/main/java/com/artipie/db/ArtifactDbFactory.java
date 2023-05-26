/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.YamlMapping;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

/**
 * Factory to create and initialize artifacts SqLite database.
 * <p/>
 * Factory accepts Artipie yaml settings file and creates database source and database structure.
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
    static final String PATH = "sqlite_data_file_path";

    /**
     * Default path to database file.
     */
    private static final String DEF_PATH = "/var/artipie/artifacts.db";

    /**
     * Settings yaml.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml Settings yaml
     */
    public ArtifactDbFactory(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Initialize artifacts database and mechanism to gather artifacts metadata and
     * write to db.
     * If some settings are absent or errors occurred while initialization, artifacts db is
     * disabled with corresponding error logged.
     * @return Queue to add artifacts metadata into
     * @throws SQLException On error
     */
    public DataSource initialize() throws SQLException {
        final YamlMapping config = this.yaml.yamlMapping("artifacts_database");
        String path = ArtifactDbFactory.DEF_PATH;
        if (config != null) {
            path = config.string(ArtifactDbFactory.PATH);
        }
        final SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl(String.format("jdbc:sqlite:%s", path));
        ArtifactDbFactory.createStructure(source);
        return source;
    }

    /**
     * Create db structure to write artifacts data.
     * @param source Database source
     * @throws SQLException On error
     */
    private static void createStructure(final DataSource source) throws SQLException {
        try (Connection conn = source.getConnection();
            Statement statement = conn.createStatement()) {
            statement.executeUpdate(
                String.join(
                    "\n",
                    "create TABLE if NOT EXISTS artifacts(",
                    "   id INTEGER PRIMARY KEY AUTOINCREMENT,",
                    "   repo_type CHAR(10) NOT NULL,",
                    "   repo_name CHAR(20) NOT NULL,",
                    "   name VARCHAR NOT NULL,",
                    "   version CHAR(20) NOT NULL,",
                    "   size DOUBLE NOT NULL,",
                    "   created_date REAL NOT NULL,",
                    "   owner VARCHAR NOT NULL,",
                    "   UNIQUE (repo_name, name, version) ",
                    ");"
                )
            );
        }
    }
}
