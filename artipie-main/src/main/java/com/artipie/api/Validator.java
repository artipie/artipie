/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.verifier.Verifier;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.function.Supplier;

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

    /**
     * Builds validator instance from condition, error message and status code.
     * @param condition Condition
     * @param message Error message
     * @param code Status code
     * @return Validator instance
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Validator validator(final Supplier<Boolean> condition,
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
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Validator validator(final Supplier<Boolean> condition,
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
     * Builds validator instance from verifier and status code.
     * @param verifier Verifier
     * @param code Status code
     * @return Validator instance
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Validator validator(final Verifier verifier, final int code) {
        return context -> {
            final boolean valid = verifier.valid();
            if (!valid) {
                context.response()
                    .setStatusCode(code)
                    .end(verifier.message());
            }
            return valid;
        };
    }

    /**
     * This validator is matched only when all of the validators are matched.
     * @since 0.26
     */
    class All implements Validator {
        /**
         * Validators.
         */
        private final Iterable<Validator> validators;

        /**
         * Validate by multiple validators.
         * @param validators Rules array
         */
        public All(final Validator... validators) {
            this(Arrays.asList(validators));
        }

        /**
         * Validate by multiple validators.
         * @param validators Validator
         */
        public All(final Iterable<Validator> validators) {
            this.validators = validators;
        }

        @Override
        public boolean validate(final RoutingContext context) {
            boolean valid = false;
            for (final Validator validator : this.validators) {
                valid = validator.validate(context);
                if (!valid) {
                    break;
                }
            }
            return valid;
        }
    }
}
