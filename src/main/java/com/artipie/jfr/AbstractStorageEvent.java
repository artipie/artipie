/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Abstract storage event.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
@StackTrace(false)
public abstract class AbstractStorageEvent extends Event {

    @Label("Storage Identifier")
    public volatile String storage;

    @Label("Key")
    public volatile String key;

}
