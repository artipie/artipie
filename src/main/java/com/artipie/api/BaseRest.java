/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.api;

import com.jcabi.log.Logger;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.function.Supplier;

/**
 * Base class for rest-api operations.
 * @since 0.26
 */
abstract class BaseRest {
    /**
     * Key 'repo' inside json-object.
     */
    protected static final String REPO = "repo";

    /**
     * Mount openapi operation implementations.
     * @param rbr RouterBuilder
     */
    public abstract void init(RouterBuilder rbr);

    /**
     * Handle error.
     * @param code Error code
     * @return Error handler
     */
    protected Handler<RoutingContext> errorHandler(final int code) {
        return context -> {
            context.response()
                .setStatusMessage(context.failure().getMessage())
                .setStatusCode(code)
                .end();
            Logger.error(this, context.failure().getMessage());
        };
    }

    /**
     * Validates by using condition and sends response in case of error.
     * @param condition Validation condition
     * @param message Error message
     * @param error Error status
     * @param context Routing context
     * @return Result of validation
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    protected static boolean validate(final Supplier<Boolean> condition,
        final String message,
        final int error,
        final RoutingContext context) {
        final boolean valid = condition.get();
        if (!valid) {
            context.response()
                .setStatusCode(error)
                .end(message);
        }
        return valid;
    }

    /**
     * Validates by using validator and sends response in case of error.
     * @param validator Validator
     * @param error Error status
     * @param context Routing context
     * @return Result of validation
     */
    protected static boolean validate(final Validator validator,
        final int error,
        final RoutingContext context) {
        final boolean valid = validator.isValid();
        if (!valid) {
            context.response()
                .setStatusCode(error)
                .end(validator.errorMessage());
        }
        return valid;
    }
}
