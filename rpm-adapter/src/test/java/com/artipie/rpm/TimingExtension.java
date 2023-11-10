/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import com.jcabi.log.Logger;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Junit extension to measure test time execution.
 * @since 1.0
 * @checkstyle JavadocVariableCheck (500 lines)
 */
@SuppressWarnings("PMD.GuardLogStatement")
public final class TimingExtension implements BeforeTestExecutionCallback,
    AfterTestExecutionCallback {

    private static final String START_TIME = "start time";

    @Override
    public void beforeTestExecution(final ExtensionContext context) {
        this.getStore(context).put(TimingExtension.START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) {
        final long start = this.getStore(context)
            .remove(TimingExtension.START_TIME, long.class);
        Logger.info(
            context.getRequiredTestClass(),
            "Method %s took %[ms]s",
            context.getRequiredTestMethod().getName(),
            System.currentTimeMillis() - start
        );
    }

    /**
     * Returns store from context.
     * @param context Extension context
     * @return Junit store
     */
    private Store getStore(final ExtensionContext context) {
        return context.getStore(
            Namespace.create(this.getClass(), context.getRequiredTestMethod())
        );
    }

}
