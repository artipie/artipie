/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import com.google.common.base.Strings;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Conan /v1/users/* REST APIs. For now minimally implemented, just for package uploading support.
 * @since 0.1
 */
public final class UsersEntity {

    /**
     * Pattern for /authenticate request.
     */
    public static final PathWrap USER_AUTH_PATH = new PathWrap.UserAuth();

    /**
     * Pattern for /check_credentials request.
     */
    public static final PathWrap CREDS_CHECK_PATH = new PathWrap.CredsCheck();

    /**
     * Error message string for the client.
     */
    private static final String URI_S_NOT_FOUND = "URI %s not found.";

    /**
     * HTTP Content-type header name.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * HTTP json application type string.
     */
    private static final String JSON_TYPE = "application/json";

    /**
     * Ctor.
     */
    private UsersEntity() {
    }

    /**
     * Conan /authenticate REST APIs.
     * @since 0.1
     */
    public static final class UserAuth implements Slice {

        /**
         * Current auth implemenation.
         */
        private final Authentication auth;

        /**
         * User token generator.
         */
        private final Tokens tokens;

        /**
         * Ctor.
         * @param auth Login authentication for the user.
         * @param tokens Auth. token genrator for the user.
         */
        public UserAuth(final Authentication auth, final Tokens tokens) {
            this.auth = auth;
            this.tokens = tokens;
        }

        @Override
        public Response response(final RequestLine line,
                                 final Headers headers, final Content body) {
            return new AsyncResponse(
                new BasicAuthScheme(this.auth).authenticate(headers).thenApply(
                    authResult -> {
                        assert authResult.status() != AuthScheme.AuthStatus.FAILED;
                        final String token = this.tokens.generate(authResult.user());
                        final Response result;
                        if (Strings.isNullOrEmpty(token)) {
                            result = new RsWithBody(
                                StandardRs.NOT_FOUND,
                                String.format(
                                    UsersEntity.URI_S_NOT_FOUND, line.uri()
                                ),
                                StandardCharsets.UTF_8
                            );
                        } else {
                            result = new RsWithHeaders(
                                new RsWithBody(
                                    StandardRs.OK, token, StandardCharsets.UTF_8
                                ),
                                UsersEntity.CONTENT_TYPE, "text/plain"
                            );
                        }
                        return result;
                    }
                )
            );
        }
    }

    /**
     * Conan /check_credentials REST APIs.
     * @since 0.1
     */
    public static final class CredsCheck implements Slice {

        @Override
        public Response response(final RequestLine line,
                                 final Headers headers, final Content body) {
            return new AsyncResponse(
                CompletableFuture.supplyAsync(line::uri).thenCompose(
                    uri -> CredsCheck.credsCheck().thenApply(
                        content -> {
                            final Response result;
                            if (Strings.isNullOrEmpty(content)) {
                                result = new RsWithBody(
                                    StandardRs.NOT_FOUND,
                                    String.format(UsersEntity.URI_S_NOT_FOUND, uri),
                                    StandardCharsets.UTF_8
                                );
                            } else {
                                result = new RsWithHeaders(
                                    new RsWithBody(
                                        StandardRs.OK, content, StandardCharsets.UTF_8
                                    ),
                                    UsersEntity.CONTENT_TYPE, UsersEntity.JSON_TYPE
                                );
                            }
                            return result;
                        }
                    )
                )
            );
        }

        /**
         * Checks user credentials for Conan HTTP request.
         * @return Json string response.
         */
        private static CompletableFuture<String> credsCheck() {
            return CompletableFuture.completedFuture("{}");
        }
    }
}
