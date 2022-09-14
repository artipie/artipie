/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.ext.web.RoutingContext;

/**
 * Validator.
 * @since 0.26
 */
@FunctionalInterface
public interface Validator {
    /**
     * Validates by using context.
     * @param context RoutingContext
     * @return Result of validation
     */
    boolean validate(RoutingContext context);
}
