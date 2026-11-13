/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HS256 secret used to sign and verify Artipie JWT tokens.
 * <p>
 * The secret is read from the {@value #ENV} environment variable. When the variable
 * is not set, a cryptographically random secret is generated for the lifetime of the
 * process, so tokens are not valid across restarts or between replicas. A configured
 * secret shorter than {@value #MIN_BYTES} bytes is rejected.
 * @since 1.0
 */
public final class JwtSecret {

    /**
     * Environment variable holding the JWT secret.
     */
    public static final String ENV = "ARTIPIE_JWT_SECRET";

    /**
     * Minimal secret length in bytes (HS256 key size).
     */
    public static final int MIN_BYTES = 32;

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtSecret.class);

    /**
     * Random source for generated secrets.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Environment to read the secret from.
     */
    private final Map<String, String> env;

    /**
     * Secret read from the process environment.
     */
    public JwtSecret() {
        this(System.getenv());
    }

    /**
     * Secret read from the given environment.
     * @param env Environment variables
     */
    public JwtSecret(final Map<String, String> env) {
        this.env = env;
    }

    /**
     * Resolve the secret value.
     * <p>
     * Every call without a configured secret generates a new random value, so callers
     * must resolve it once and share it between token issuers and verifiers.
     * @return Secret value
     * @throws IllegalStateException If the configured secret is too short
     */
    public String value() {
        final String configured = this.env.get(JwtSecret.ENV);
        final String res;
        if (configured == null || configured.isBlank()) {
            LOGGER.warn(
                "{} is not set, using a random JWT secret: tokens are valid only for this process",
                JwtSecret.ENV
            );
            final byte[] bytes = new byte[JwtSecret.MIN_BYTES];
            JwtSecret.RANDOM.nextBytes(bytes);
            res = Base64.getEncoder().encodeToString(bytes);
        } else if (configured.getBytes(StandardCharsets.UTF_8).length < JwtSecret.MIN_BYTES) {
            throw new IllegalStateException(
                String.format(
                    "%s must be at least %d bytes long", JwtSecret.ENV, JwtSecret.MIN_BYTES
                )
            );
        } else {
            res = configured;
        }
        return res;
    }
}
