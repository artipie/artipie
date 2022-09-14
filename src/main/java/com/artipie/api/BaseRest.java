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
     * Builds validator instance from condition, error message and status code.
     * @param condition Condition
     * @param message Error message
     * @param code Status code
     * @return Validator instance
     */
    protected static Validator validator(final Supplier<Boolean> condition,
        final String message, final int code) {
        return context -> {
            final boolean valid = condition.get();
            if (!valid) {
                context.response()
                    .setStatusCode(code)
                    .end(message);
            }
            return valid;
        };
    }

    /**
     * Builds validator instance from condition, error message and status code.
     * @param condition Condition
     * @param message Error message
     * @param code Status code
     * @return Validator instance
     */
    protected static Validator validator(final Supplier<Boolean> condition,
        final Supplier<String> message, final int code) {
        return context -> {
            final boolean valid = condition.get();
            if (!valid) {
                context.response()
                    .setStatusCode(code)
                    .end(message.get());
            }
            return valid;
        };
    }

    /**
     * Builds composed validator from other validators.
     * @param validators Validators
     * @return Composed validator
     */
    protected static Validator validator(final Validator... validators) {
        return context -> {
            boolean valid = false;
            for (final Validator validator : validators) {
                valid = validator.validate(context);
                if (!valid) {
                    break;
                }
            }
            return valid;
        };
    }
}
