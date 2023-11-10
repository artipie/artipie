/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.conan;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Tokenize repository items via JWT tokens.
 * @since 0.1
 */
public class ItemTokenizer {

    /**
     * Field name for host name property of the repository item.
     */
    private static final String HOSTNAME = "hostname";

    /**
     * Field name for path value property of the repository item.
     */
    private static final String PATH = "path";

    /**
     * Basic interface for creating JWT objects.
     */
    private final JWTAuth provider;

    /**
     * Create new instance with JWT support via vertx instance.
     * @param vertx Vertx core instance.
     */
    public ItemTokenizer(final Vertx vertx) {
        this.provider = JWTAuth.create(
            vertx, new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("some secret")
            )
        );
    }

    /**
     * Generates string token for repository item info provided.
     * @param path Path value property of the repository item.
     * @param hostname Host name property of the repository item.
     * @return Java String token in JWT format.
     */
    public String generateToken(final String path, final String hostname) {
        final String token = this.provider.generateToken(
            new JsonObject().put(ItemTokenizer.PATH, path)
                .put(ItemTokenizer.HOSTNAME, hostname)
        );
        return token;
    }

    /**
     * Authenticate by token and decode item data.
     * @param token Item token string.
     * @return Decoded item data.
     */
    public CompletionStage<Optional<ItemInfo>> authenticateToken(final String token) {
        final CompletionStage<Optional<ItemInfo>> item = this.provider.authenticate(
            new TokenCredentials(token)
        ).map(
            user -> {
                final JsonObject principal = user.principal();
                Optional<ItemInfo> res = Optional.empty();
                if (principal.containsKey(ItemTokenizer.PATH)
                    && user.containsKey(ItemTokenizer.HOSTNAME)) {
                    res = Optional.of(
                        new ItemInfo(
                            principal.getString(ItemTokenizer.PATH),
                            principal.getString(ItemTokenizer.HOSTNAME).toString()
                        )
                    );
                }
                return res;
            }
        ).toCompletionStage();
        return item;
    }

    /**
     * Repository item info.
     * @since 0.1
     */
    public static final class ItemInfo {

        /**
         * Path to the item.
         */
        private final String path;

        /**
         * Host name of the client.
         */
        private final String hostname;

        /**
         * Ctor.
         * @param path Path to the item.
         * @param hostname Host name of the client.
         */
        public ItemInfo(final String path, final String hostname) {
            this.path = path;
            this.hostname = hostname;
        }

        /**
         * Path to the item.
         * @return Path to the item.
         */
        public String getPath() {
            return this.path;
        }

        /**
         * Host name of the client.
         * @return Host name of the client.
         */
        public String getHostname() {
            return this.hostname;
        }
    }
}
