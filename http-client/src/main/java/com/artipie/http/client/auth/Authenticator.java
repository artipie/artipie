/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Authenticator for HTTP requests.
 *
 * @since 0.3
 */
public interface Authenticator {

    /**
     * Anonymous authorization. Always returns empty headers set.
     */
    Authenticator ANONYMOUS = ignored -> CompletableFuture.completedFuture(Headers.EMPTY);

    /**
     * Get authorization headers.
     *
     * @param headers Headers with requirements for authorization.
     * @return Authorization headers.
     */
    CompletionStage<Headers> authenticate(Headers headers);
}
