/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.api;

import com.jcabi.log.Logger;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Handler for handling errors in routing context.
 * @since 0.26
 */
public class ErrorHandler implements Handler<RoutingContext> {

    private final int code;

    /**
     * Constructor for ErrorHandler.
     * @param code Error code
     */
    public ErrorHandler(int code) {
        this.code = code;
    }

    @Override
    public void handle(RoutingContext context) {
        if (context.failure() instanceof HttpException) {
            context.response()
                    .setStatusMessage(context.failure().getMessage())
                    .setStatusCode(((HttpException) context.failure()).getStatusCode())
                    .end();
        } else {
            context.response()
                    .setStatusMessage(context.failure().getMessage())
                    .setStatusCode(code)
                    .end();
        }
        Logger.error(this, context.failure().getMessage());
    }
}
