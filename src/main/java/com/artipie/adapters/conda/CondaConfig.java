/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.adapters.conda;

import com.amihaiemil.eoyaml.YamlMapping;
import java.time.Duration;
import java.util.Optional;

/**
 * Conda repository specific settings.
 * @since 0.23
 */
public final class CondaConfig {

    /**
     * Default token ttl.
     */
    private static final Duration DEF_TTL = Duration.ofDays(365);

    /**
     * Default time to clean expired auth tokens.
     */
    private static final String DEF_CRON = "0 0 1 * * ?";

    /**
     * Setting configuration yaml mapping from repo config.
     */
    private final Optional<YamlMapping> settings;

    /**
     * Ctor.
     * @param settings Setting configuration yaml mapping
     */
    public CondaConfig(final Optional<YamlMapping> settings) {
        this.settings = settings;
    }

    /**
     * Anaconda repository authentication tokens time to leave.
     * If setting is not present, default value of 365 days is used.
     * Format of the setting should compliant with ISO-8601 duration format PnDTnHnMn.nS and
     * {@link Duration#parse(CharSequence)} specification.
     * @return Auth tokens time to leave
     */
    public Duration authTokenTtl() {
        return this.settings.map(
            yaml -> Optional.ofNullable(yaml.string("auth_token_ttl")).map(Duration::parse)
                .orElse(CondaConfig.DEF_TTL)
        ).orElse(CondaConfig.DEF_TTL);
    }

    /**
     * Time to clean expired auth tokens as a cron expression.
     * Default value is 0 0 1 * * ? - at 01 AM every night.
     * Read more about cron expression
     * <a href="https://docs.oracle.com/cd/E12058_01/doc/doc.1014/e12030/cron_expressions.htm">here</a>.
     * @return Cron expression to clean tokens at
     */
    public String cleanAuthTokens() {
        return this.settings.map(
            yaml -> Optional.ofNullable(yaml.string("clean_auth_token_at"))
                .orElse(CondaConfig.DEF_CRON)
        ).orElse(CondaConfig.DEF_CRON);
    }

}
