/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Event triggered when storage is created.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@Name("artipie.StorageCreate")
@Label("Storage Create")
@Category({"Artipie", "Storage"})
@Description("Event triggered when storage is created")
@StackTrace(false)
public class StorageCreateEvent extends Event {

    @Label("Storage Identifier")
    public volatile String storage;

}
