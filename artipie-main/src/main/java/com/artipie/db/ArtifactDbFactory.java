/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Factory to create and initialize artifacts PostgreSQL database.
 * <p/>
 * Factory accepts Artipie yaml settings file and creates database source and database structure.
 * If settings are absent in config yaml, default PostgreSQL connection parameters are used.
 * <p/>
 * Artifacts db settings section in artipie yaml:
 * <pre>{@code
 * artifacts_database:
 *   postgres_host: localhost # required, PostgreSQL host
 *   postgres_port: 5432 # optional, PostgreSQL port, default 5432
 *   postgres_database: artipie # required, PostgreSQL database name
 *   postgres_user: artipie # required, PostgreSQL username
 *   postgres_password: artipie # required, PostgreSQL password
 *   threads_count: 3 # default 1, not required, in how many parallel threads to
 *       process artifacts data queue
 *   interval_seconds: 5 # default 1, not required, interval to check events queue and write into db
 * }</pre>
 * @since 0.31
 */
public final class ArtifactDbFactory {

    /**
     * PostgreSQL host configuration key.
     */
    public static final String YAML_HOST = "postgres_host";

    /**
     * PostgreSQL port configuration key.
     */
    public static final String YAML_PORT = "postgres_port";

    /**
     * PostgreSQL database configuration key.
     */
    public static final String YAML_DATABASE = "postgres_database";

    /**
     * PostgreSQL user configuration key.
     */
    public static final String YAML_USER = "postgres_user";

    /**
     * PostgreSQL password configuration key.
     */
    public static final String YAML_PASSWORD = "postgres_password";

    /**
     * Default PostgreSQL host.
     */
    static final String DEFAULT_HOST = "localhost";

    /**
     * Default PostgreSQL port.
     */
    static final int DEFAULT_PORT = 5432;

    /**
     * Default PostgreSQL database name.
     */
    static final String DEFAULT_DATABASE = "artifacts";

    /**
     * Settings yaml.
     */
    private final YamlMapping yaml;

    /**
     * Default database name if not specified in config.
     */
    private final String defaultDb;

    /**
     * Ctor.
     * @param yaml Settings yaml
     * @param defaultDb Default database name
     */
    public ArtifactDbFactory(final YamlMapping yaml, final String defaultDb) {
        this.yaml = yaml;
        this.defaultDb = defaultDb;
    }

    /**
     * Initialize artifacts database and mechanism to gather artifacts metadata and
     * write to db.
     * If yaml settings are absent, default PostgreSQL connection parameters are used.
     * @return Queue to add artifacts metadata into
     * @throws ArtipieException On error
     */
    public DataSource initialize() {
        final YamlMapping config = this.yaml.yamlMapping("artifacts_database");
        
        final String host = config != null && config.string(ArtifactDbFactory.YAML_HOST) != null 
            ? config.string(ArtifactDbFactory.YAML_HOST) 
            : ArtifactDbFactory.DEFAULT_HOST;
            
        final int port = config != null && config.string(ArtifactDbFactory.YAML_PORT) != null 
            ? Integer.parseInt(config.string(ArtifactDbFactory.YAML_PORT)) 
            : ArtifactDbFactory.DEFAULT_PORT;
            
        final String database = config != null && config.string(ArtifactDbFactory.YAML_DATABASE) != null 
            ? config.string(ArtifactDbFactory.YAML_DATABASE) 
            : this.defaultDb;
            
        final String user = config != null && config.string(ArtifactDbFactory.YAML_USER) != null 
            ? config.string(ArtifactDbFactory.YAML_USER) 
            : "artipie";
            
        final String password = config != null && config.string(ArtifactDbFactory.YAML_PASSWORD) != null 
            ? config.string(ArtifactDbFactory.YAML_PASSWORD) 
            : "artipie";
        
        final PGSimpleDataSource source = new PGSimpleDataSource();
        source.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        source.setUser(user);
        source.setPassword(password);
        
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
                    "CREATE TABLE IF NOT EXISTS artifacts(",
                    "   id BIGSERIAL PRIMARY KEY,",
                    "   repo_type CHAR(10) NOT NULL,",
                    "   repo_name CHAR(20) NOT NULL,",
                    "   name VARCHAR NOT NULL,",
                    "   version VARCHAR NOT NULL,",
                    "   size BIGINT NOT NULL,",
                    "   created_date BIGINT NOT NULL,",
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
